package com.test.server;

import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;
import com.test.MyOpCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * 心跳包处理
 *
 * @author zkpursuit
 */
@Handler(cmd = MyOpCode.heart_beat, pooledSize = 8) // 使用自定义数据包时的心跳处理，此时的心跳协议号为整型数据
@Handler(cmd = MyOpCode.heart_beat, type = String.class, pooledSize = 8) // 使用rpc处理心跳包，此时的心跳协议号为字符串类型
public class HeartBeatServerHandler extends RpcMessageHandler {

    @Override
    public Object execute(RpcMessage message) {
        if (message.getWhat() instanceof String) { //协议号为字符串类型表示rpc调用
            return System.currentTimeMillis(); //rpc调用返回具体数据
        }
        //以下使用自定义数据包处理心跳，客户端处理服务端的心跳返回 请参看 com.test.client.HeartBeatClientHandler 类
        ChannelHandlerContext ctx = message.getCtx();

        //Long serverId = NettyCtxManager.getServerId(ctx);
        //System.out.println("serverId:" + serverId + ", clientCount:" + NettyCtxManager.instance().getClientCount(serverId));

        ByteBuf sendBuf = ctx.alloc().buffer(12);
        int opcode = Integer.parseInt(String.valueOf(this.cmd()));
        sendBuf.writeInt(opcode); //如同进程下同时启动服务端和客户端时，请务必使用不同的opcode协议号
        sendBuf.writeLong(System.currentTimeMillis());
        ctx.writeAndFlush(sendBuf);
        return null; // 非rpc调用时直接返回null
    }
}

