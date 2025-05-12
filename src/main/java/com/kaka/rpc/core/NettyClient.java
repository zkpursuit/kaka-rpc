package com.kaka.rpc.core;

import ch.qos.logback.classic.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.rpc.codec.LengthDataDecoder;
import com.kaka.rpc.codec.LengthDataEncoder;
import com.kaka.util.JdkSerializer;
import com.kaka.util.MathUtils;
import com.kaka.util.NanoId;
import com.kaka.util.Serializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.kaka.rpc.core.RpcOpCode.err_code;


/**
 * 基于Netty tcp客户端
 *
 * @author zkpursuit
 */
abstract public class NettyClient implements IClient {

    final static class RpcInvoke {
        final CompletableFuture<Object> future;
        final Timeout timeout;

        public RpcInvoke(CompletableFuture<Object> future, Timeout timeout) {
            this.future = future;
            this.timeout = timeout;
        }

        public CompletableFuture<Object> getFuture() {
            return future;
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public void cancel() {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            if (timeout != null && !timeout.isCancelled()) {
                timeout.cancel();
            }
        }
    }

    protected final Logger logger;
    protected final Facade facade;
    protected final int connectTimeout;
    protected final int readTimeout;
    protected final int heartBeatInterval;
    protected final int reconnectInterval;
    private final InetSocketAddress address; //服务器地址列表
    private final AtomicReference<ChannelHandlerContext> ctxRef;
    private Bootstrap bootstrap;
    private ChannelFuture channelFuture;
    private ChannelFuture channelCloseFuture;
    private boolean connected;
    private final static Cache<String, RpcInvoke> rpcInvokeCache = Caffeine.newBuilder()
            .initialCapacity(16)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .evictionListener((id, val, cause) -> {
                if (val != null) {
                    ((RpcInvoke) val).cancel();
                }
            })
            .build();
    private final static int rpcTimeoutSecs = Integer.parseInt(System.getProperty("rpc_timeout_seconds", "5"));
    private final static HashedWheelTimer timer = new HashedWheelTimer();
    final static int cmd_sc_sync_result = Integer.parseInt(RpcOpCode.sc_rpc);
    public static Serializer<Object> serializer = new JdkSerializer();

    /**
     * 构造方法
     *
     * @param facade            事件总线
     * @param address           tcp服务器地址
     * @param connectTimeout    连接超时（秒）
     * @param readTimeout       读超时（秒）
     * @param heartBeatInterval 发送心跳包的间隔时间（秒）
     * @param reconnectInterval 断线重连间隔时间（秒）
     */
    public NettyClient(Facade facade, InetSocketAddress address, int connectTimeout, int readTimeout, int heartBeatInterval, int reconnectInterval) {
        this.logger = (Logger) LoggerFactory.getLogger(this.getClass());
        this.facade = facade == null ? FacadeFactory.getFacade() : facade;
        this.address = address;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.heartBeatInterval = heartBeatInterval;
        this.reconnectInterval = reconnectInterval;
        this.ctxRef = new AtomicReference<>();
    }

    /**
     * 构造方法
     *
     * @param address           tcp服务器地址
     * @param connectTimeout    连接超时（秒）
     * @param readTimeout       读超时（秒）
     * @param heartBeatInterval 发送心跳包的间隔时间（秒）
     * @param reconnectInterval 断线重连间隔时间（秒）
     */
    public NettyClient(InetSocketAddress address, int connectTimeout, int readTimeout, int heartBeatInterval, int reconnectInterval) {
        this(null, address, connectTimeout, readTimeout, heartBeatInterval, reconnectInterval);
    }

    /**
     * 获取服务器地址
     *
     * @return 所连接的服务器地址
     * @see InetSocketAddress
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * 获取连接
     *
     * @return 连接
     */
    public ChannelHandlerContext getChannelHandlerContext() {
        ChannelHandlerContext ctx = ctxRef.get();
        if (ctx == null || ctx.isRemoved()) {
            return null;
        }
        Channel channel = ctx.channel();
        if (!channel.isRegistered() || !channel.isOpen() || !channel.isActive()) {
            return null;
        }
        return ctx;
    }

    /**
     * 发送心跳包
     */
    abstract protected void ping();

    /**
     * 连接成功后
     */
    abstract protected void afterConnected();

    /**
     * 连接断开
     */
    abstract protected void afterDisconnect();

    /**
     * 通信协议错误码处理
     *
     * @param triggerErrorOpcode 触发错误协议号
     * @param errLevel           错误级别
     * @param errCode            错误码
     * @param errInfo            错误信息
     */
    abstract protected void onErrorCodeListener(int triggerErrorOpcode, int errLevel, int errCode, String errInfo);

    /**
     * 处理数据包
     *
     * @param opcode 协议号
     * @param msg    数据包内容
     */
    protected void processDataPacket(int opcode, ByteBuf msg) {
        if (opcode == cmd_sc_sync_result) {
            short idLen = msg.readShort();
            byte[] idBytes = new byte[idLen];
            msg.readBytes(idBytes);
            String id = new String(idBytes, StandardCharsets.UTF_8);
            RpcInvoke rpcInvoke = getRpcInvoke(id);
            if (rpcInvoke == null) return;
            short cmdLen = msg.readShort();
            byte[] cmdBytes = new byte[cmdLen];
            msg.readBytes(cmdBytes); //读取协议号
            int returnType = msg.readByte();
            if (rpcInvoke.getTimeout() != null) {
                rpcInvoke.getTimeout().cancel();
            }
            CompletableFuture<Object> future = rpcInvoke.getFuture();
            Object returnVal = Utils.readValue(msg, serializer);
            if (returnType == -1) {
                String exInfo;
                if (returnVal instanceof String) {
                    exInfo = (String) returnVal;
                } else {
                    byte[] bytes = (byte[]) returnVal;
                    exInfo = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
                }
                future.completeExceptionally(new RemoteException(exInfo));
            } else {
                //正常结果
                try {
                    future.complete(returnVal);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        } else if (opcode == err_code) {
            int triggerErrorOpcode = msg.readInt();
            int errLevel = msg.readByte();
            int errCode = msg.readInt();
            int errInfoLen = msg.readInt();
            byte[] errInfoBytes = new byte[errInfoLen];
            msg.readBytes(errInfoBytes);
            String errInfo = new String(errInfoBytes, StandardCharsets.UTF_8);
            this.onErrorCodeListener(triggerErrorOpcode, errLevel, errCode, errInfo);
        } else {
            //处理来自服务器的协议事件
            facade.sendMessage(new RpcMessage(opcode, msg, this));
        }
    }

    /**
     * 初始化编解码器
     */
    protected void initCodec(ChannelPipeline pipeline) {
        pipeline.addLast(new LengthDataDecoder(2048)); //自定义解码器
        pipeline.addLast(new LengthDataEncoder()); //自定义编码器
    }

    /**
     * 初始化连接处理器，包括数据包处理，心跳超时处理
     *
     * @return 连接处理器
     */
    protected ChannelHandler buildHandler() {
        return new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                int opcode = msg.readInt();
                processDataPacket(opcode, msg);
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
                if (!ctx.isRemoved()) {
                    ctxRef.set(ctx);
                    connected = true;
                    afterConnected();
                }
            }

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                super.userEventTriggered(ctx, evt);
                if (evt instanceof IdleStateEvent e) {
                    if (null != e.state()) switch (e.state()) {
                        case READER_IDLE:
                            if (logger.isInfoEnabled()) {
                                logger.info("读超时，重连……");
                            }
                            closeChannel(ctx); //读超时关闭连接后将自动重连
                            break;
                        case WRITER_IDLE:
                            ping();
                            break;
                        default:
                            break;
                    }
                }
            }
        };
    }

    /**
     * 初始化连接参数
     *
     * @param readerIdleMilliseconds 读超时，超出此时间将重连
     * @param writerIdleMilliseconds 写超时，超出此时间将发送心跳包
     * @return netty连接初始化器
     */
    ChannelInitializer<SocketChannel> buildChannelInitializer(int readerIdleMilliseconds, int writerIdleMilliseconds) {
        return new ChannelInitializer<>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (readerIdleMilliseconds > 0 || writerIdleMilliseconds > 0) {
                    int readerIdleTime = Math.max(readerIdleMilliseconds, 0);
                    int writerIdleTime = Math.max(writerIdleMilliseconds, 0);
                    pipeline.addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, 0, TimeUnit.MILLISECONDS));
                }
                initCodec(pipeline);
                pipeline.addLast(buildHandler());
            }
        };
    }

    /**
     * 连接服务器
     */
    private void connect0() {
        try {
            new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4);
            this.bootstrap = new Bootstrap();
            this.bootstrap.group(new NioEventLoopGroup())
                    .channel(NioSocketChannel.class)
                    .remoteAddress(this.address)
                    .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                    //.option(ChannelOption.WRITE_SPIN_COUNT, 1000)
                    //.option(ChannelOption.SO_RCVBUF, 1048576)
                    //.option(ChannelOption.SO_SNDBUF, 1048576)
                    //.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
                    .handler(this.buildChannelInitializer(readTimeout * 1000, heartBeatInterval * 1000));
            this.channelFuture = bootstrap.connect().sync();
            this.channelCloseFuture = this.channelFuture.channel().closeFuture().sync();
        } catch (Exception ex) {
            logger.error(ex.getLocalizedMessage());
        } finally {
            this.shutdown();
            try {
                Thread.sleep(reconnectInterval * 1000L);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            if (logger.isInfoEnabled()) logger.info("连接异常，重连……");
            this.connect0();
        }
    }

    /**
     * 关闭连接
     *
     * @param ctx 连接
     */
    private void closeChannel(ChannelHandlerContext ctx) {
        ChannelHandlerContext ctx0 = ctxRef.get();
        if (ctx0 != null) {
            this.ctxRef.set(null);
            ctx0.channel().close();
            ctx0.close();
        }
        if (ctx != null && ctx != ctx0) {
            ctx.channel().close();
            ctx.close();
        }
        if (connected) {
            connected = false;
            afterDisconnect();
        }
    }

    /**
     * 连接服务器，此方法已被线程包裹
     */
    public void connect() {
        new Thread(this::connect0).start();
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        this.closeChannel(null);
        if (this.bootstrap == null) return;
        EventLoopGroup group = this.bootstrap.config().group();
        if (group != null) group.shutdownGracefully();
        if (this.channelCloseFuture != null) this.channelCloseFuture.channel().close();
        if (this.channelFuture != null) this.channelFuture.channel().close();
    }

    /**
     * 是否可以收发数据
     *
     * @return true表示可以收发数据
     */
    public boolean isActive() {
        return this.getChannelHandlerContext() != null;
    }

    /**
     * 发送自定义数据包
     *
     * @param data 数据包，结构为：int协议号+协议内容
     * @return true表示发送成功
     */
    @Override
    public boolean sendAndFlush(ByteBuf data) {
        ChannelHandlerContext ctx = this.getChannelHandlerContext();
        if (ctx == null) return false;
        ctx.writeAndFlush(data);
        return true;
    }

    /**
     * 缓存异步结果对象，用于等待异步消息为结果对象赋值
     *
     * @param id     异步结果对象唯一标识
     * @param result 异步结果对象
     */
    void cacheRpcInvoke(String id, RpcInvoke result) {
        rpcInvokeCache.put(id, result);
    }

    /**
     * 从异步结果对象缓存池中获取异步结果对象
     *
     * @param id 异步结果对象唯一标识
     * @return 异步结果对象
     */
    RpcInvoke getRpcInvoke(String id) {
        RpcInvoke val = rpcInvokeCache.getIfPresent(id);
        rpcInvokeCache.invalidate(id);
        return val;
    }

    RpcInvoke removeRpcInvoke(String id) {
        RpcInvoke val = rpcInvokeCache.getIfPresent(id);
        rpcInvokeCache.invalidate(id);
        return val;
    }

    /**
     * 请求远程服务器处理领域事件，{@link RpcInvokeHandler}
     *
     * @param id     前后端通信数据包唯一标识
     * @param cmd    领域事件名
     * @param params 领域事件参数
     */
    protected void remotingRequest(ChannelHandlerContext ctx, String id, String cmd, Object[] params) {
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        byte[] cmdBytes = cmd.getBytes(StandardCharsets.UTF_8);
        int bufCapacity = MathUtils.nextPowerOfTwo(10 + idBytes.length + cmdBytes.length);
        ByteBuf buf = ctx.alloc().buffer(bufCapacity);
        buf.writeInt(RpcInvokeHandler.opcode);
        buf.writeShort(idBytes.length);
        buf.writeBytes(idBytes);
        buf.writeShort(cmdBytes.length);
        buf.writeBytes(cmdBytes);
        if (params == null || params.length == 0) {
            buf.writeShort(0);
        } else {
            buf.writeShort(params.length);
            for (Object param : params) {
                Utils.writeValue(buf, param, serializer);
            }
        }
        ctx.writeAndFlush(buf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Object> execRemotingLogic0(final String cmd, final int timeoutSeconds, final Object[] params) throws Exception {
        ChannelHandlerContext ctx = this.getChannelHandlerContext();
        if (ctx == null) {
            throw new ConnectException("Disconnect from the server");
        }
        final String id = NanoId.randomNanoId();
        Timeout timeout = null;
        if (timeoutSeconds > 0) {
            timer.newTimeout(t -> {
                if (!t.isCancelled()) {
                    RpcInvoke rpcInvoke = removeRpcInvoke(id);
                    if (rpcInvoke != null) {
                        CompletableFuture<?> future = rpcInvoke.getFuture();
                        if (future != null) {
                            future.completeExceptionally(new TimeoutException("请求超时"));
                        }
                    }
                }
            }, timeoutSeconds, TimeUnit.SECONDS);
        }
        CompletableFuture<Object> future = new CompletableFuture<>();
        this.cacheRpcInvoke(id, new RpcInvoke(future, timeout));
        this.remotingRequest(ctx, id, cmd, params);
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Object> execRemotingLogic0(final String cmd, final Object[] params) throws Exception {
        return execRemotingLogic0(cmd, 0, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execRemotingLogic(final String cmd, final int timeoutSeconds, final Object[] params) throws Exception {
        CompletableFuture<Object> future = this.execRemotingLogic0(cmd, params);
        Object resultVal = future.get(timeoutSeconds, TimeUnit.SECONDS);
        if (resultVal instanceof RemoteException) {
            throw (RemoteException) resultVal;
        }
        return (T) resultVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execRemotingLogic(final String cmd, Object... params) throws Exception {
        return this.execRemotingLogic(cmd, rpcTimeoutSecs, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execRemotingLogic(final String cmd, final int timeoutSeconds, final BiConsumer<Object, ? super Throwable> action, Object... params) {
        if (action == null) {
            throw new IllegalArgumentException("The result processor cannot be null");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("The timeout period must be greater than 0");
        }
        try {
            CompletableFuture<Object> future = this.execRemotingLogic0(cmd, timeoutSeconds, params);
            future.thenAccept(res -> action.accept(res, null)).exceptionally(e -> {
                action.accept(null, e);
                return null;
            });
        } catch (Exception e) {
            action.accept(null, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execRemotingLogic(final String cmd, final BiConsumer<Object, ? super Throwable> action, Object... params) {
        this.execRemotingLogic(cmd, rpcTimeoutSecs, action, params);
    }

}
