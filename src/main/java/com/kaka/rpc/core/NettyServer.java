package com.kaka.rpc.core;

import ch.qos.logback.classic.Logger;
import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.rpc.codec.LengthDataDecoder;
import com.kaka.rpc.codec.LengthDataEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的tcp服务器
 *
 * @author zkpursuit
 */
public class NettyServer {
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected EventLoopGroup businessThreadGroup;
    private final Logger logger = (Logger) LoggerFactory.getLogger(NettyServer.class);
    static ProtocolPreHandler protocolPreHandler;

    /**
     * 构造方法
     *
     * @param protocolPreHandler 协议预处理器
     */
    public NettyServer(ProtocolPreHandler protocolPreHandler) {
        NettyServer.protocolPreHandler = protocolPreHandler;
        Facade facade = FacadeFactory.getFacade();
        if (!facade.hasCommand(RpcInvokeHandler.opcode)) {
            facade.registerCommand(RpcInvokeHandler.opcode, RpcInvokeHandler.class);
        }
    }

    public NettyServer() {
        this(null);
    }

    /**
     * 启动服务
     *
     * @param address                绑定的地址
     * @param ioThreadPoolSize       IO线程池大小，处理read/write
     * @param businessThreadPoolSize 业务处理线程池中默认初始化线程
     * @param readerIdleMilliseconds 读超时时间，0表示不可用
     * @param writerIdleMilliseconds 写超时时间，0表示不可用
     * @param allIdleMilliseconds    读写超时时间，0表示不可用
     */
    public void start(InetSocketAddress address, int ioThreadPoolSize, int businessThreadPoolSize, int readerIdleMilliseconds, int writerIdleMilliseconds, int allIdleMilliseconds) {
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(ioThreadPoolSize);
            if (businessThreadPoolSize > 0) {
                businessThreadGroup = new NioEventLoopGroup(businessThreadPoolSize);
            }
            ServerBootstrap bootstrap = new ServerBootstrap();
            //bootstrap.option(ChannelOption.SO_TIMEOUT, this);
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(buildChannelInitializer(readerIdleMilliseconds, writerIdleMilliseconds, allIdleMilliseconds))
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 设置连接超时时间
                    .option(ChannelOption.SO_TIMEOUT, 10000)
                    .option(ChannelOption.SO_RCVBUF, 1048576)
                    .option(ChannelOption.SO_SNDBUF, 1048576)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(64, 1024, 65536 * 1024));
            ChannelFuture f = bootstrap.bind(address).sync();
            f.addListener(future -> {
                if (f.isSuccess()) {
                    logger.warn("监听端口{}成功", address.getPort());
                } else {
                    logger.warn("监听端口{}失败", address.getPort());
                }
            });
            f.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            if (businessThreadGroup != null) {
                businessThreadGroup.shutdownGracefully();
            }
        }
    }

    /**
     * 初始化编解码器
     */
    protected void initCodec(ChannelPipeline pipeline) {
        pipeline.addLast(new LengthDataDecoder(2048));
        pipeline.addLast(new LengthDataEncoder());
    }

    /**
     * 构建socket信道<br>
     * readerIdleTimeSeconds、writerIdleTimeSeconds、allIdleTimeSeconds三个参数同时为负数时将不做心跳超时处理
     *
     * @param readerIdleMilliseconds 指定时间段内未执行读操作，将触发读超时事件
     * @param writerIdleMilliseconds 指定时间段内未执行写操作，将触发写超时事件
     * @param allIdleMilliseconds    指定时间段内，既未执行读操作，也未执行写操作，将触发读写超时事件
     * @return socket信道初始化器
     */
    ChannelInitializer<SocketChannel> buildChannelInitializer(int readerIdleMilliseconds, int writerIdleMilliseconds, int allIdleMilliseconds) {
        return new ChannelInitializer<>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                if (readerIdleMilliseconds > 0 || writerIdleMilliseconds > 0 && allIdleMilliseconds > 0) {
                    int readerIdleTime = Math.max(readerIdleMilliseconds, 0);
                    int writerIdleTime = Math.max(writerIdleMilliseconds, 0);
                    int allIdleTime = Math.max(allIdleMilliseconds, 0);
                    ch.pipeline().addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime, TimeUnit.MILLISECONDS));
                }
                initCodec(ch.pipeline());
                NettyServerHandler handler = new NettyServerHandler(this.hashCode());
                if (businessThreadGroup == null) {
                    ch.pipeline().addLast(handler);
                } else {
                    ch.pipeline().addLast(businessThreadGroup, "business", handler);
                }
            }
        };
    }

    /**
     * 停止服务
     */
    public void stop() {
        if (bossGroup != null) {
            if (!bossGroup.isShutdown() && !bossGroup.isShuttingDown()) {
                bossGroup.shutdownGracefully();
            }
        }
        if (workerGroup != null) {
            if (!workerGroup.isShutdown() && !workerGroup.isShuttingDown()) {
                workerGroup.shutdownGracefully();
            }
        }
        if (businessThreadGroup != null) {
            if (!businessThreadGroup.isShutdown() && !businessThreadGroup.isShuttingDown()) {
                businessThreadGroup.shutdownGracefully();
            }
        }
    }

}
