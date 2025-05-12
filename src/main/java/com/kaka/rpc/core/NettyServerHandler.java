package com.kaka.rpc.core;

import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.notice.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 服务端通信处理器以及连接状态处理
 *
 * @author zkpursuit
 */
class NettyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    protected final Facade facade = FacadeFactory.getFacade();
    public final long serverId;

    public NettyServerHandler(long serverId) {
        this.serverId = serverId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int opcode = msg.readInt();
        if (NettyServer.protocolPreHandler != null) {
            boolean retVal = NettyServer.protocolPreHandler.handler(opcode, msg, ctx);
            if (!retVal) {
                return;
            }
        }
        facade.sendMessage(new RpcMessage(opcode, Unpooled.copiedBuffer(msg), ctx), Message.ExecuteType.ASYN_THREAD);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(NettyCtxManager.server_id).set(serverId);
        NettyCtxManager.instance().bind(ctx, "og:" + NettyCtxManager.getRemoteAddress(ctx));
        facade.sendMessage(new Message(NettyStateCode.CHANNEL_ACTIVE, ctx));
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        facade.sendMessage(new Message(NettyStateCode.CHANNEL_EXCEPTION, new Object[]{ctx, cause}));
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        facade.sendMessage(new Message(NettyStateCode.CHANNEL_REMOVE, ctx));
        super.handlerRemoved(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent e) {
            if (null != e.state()) switch (e.state()) {
                case READER_IDLE:
                    //读超时
                    facade.sendMessage(new Message(NettyStateCode.CHANNEL_READER_IDLE, ctx));
                    break;
                case WRITER_IDLE:
                    //写超时
                    facade.sendMessage(new Message(NettyStateCode.CHANNEL_WRITER_IDLE, ctx));
                    break;
                case ALL_IDLE:
                    facade.sendMessage(new Message(NettyStateCode.CHANNEL_RW_IDLE, ctx));
                    break;
                default:
                    break;
            }
        }
    }

}
