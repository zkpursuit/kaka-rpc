package com.test.client;

import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;
import com.test.MyOpCode;
import io.netty.buffer.ByteBuf;

/**
 * 客户端心跳处理，仅在使用自定义数据包时使用，如同进程下同时启动服务端和客户端时，请务必使用不同的cmd协议号
 */
@Handler(cmd = MyOpCode.heart_beat)
public class HeartBeatClientHandler extends RpcMessageHandler {
    @Override
    public Object execute(RpcMessage message) {
        ByteBuf data = (ByteBuf) message.getBody();
        long serverTime = data.readLong(); //获取服务器发送过来的时间
        //System.out.println("server time:" + serverTime);
        //获取客户端对象，如果服务器发送的数据包含了连接的客户端数量，还可以在集群模式下做更精细的负载均衡
        //NettyTcpClient client = message.getClient();
        return null;
    }
}
