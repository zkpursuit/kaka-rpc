package com.kaka.rpc.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * 通信协议前置处理器 <br>
 *
 * @author zkpursuit
 * @see com.kaka.rpc.core.NettyServer
 */
public interface ProtocolPreHandler {

    /**
     * 通信协议前置处理 <br>
     * 此方法可用于通信协议拦截处理或鉴权，也可用于rpc调用鉴权
     *
     * @param opcode 通信协议号
     * @param param  当为{@link ByteBuf}对象时，则此参数为通信数据包剩下的字节数据，当为String对象时，则此参数为rpc调用方法名，或rpc事件名
     * @param ctx    通信信道
     * @return true表示处理成功，false表示处理失败，则后续处理将不再执行
     * @throws Exception 当rpc调用抛出时，异常由rpc调用流程捕获处理。
     */
    boolean handler(int opcode, Object param, ChannelHandlerContext ctx) throws Exception;

}
