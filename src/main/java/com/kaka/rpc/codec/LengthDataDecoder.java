package com.kaka.rpc.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 通信协议解码器，用于处理粘包拆包问题 <br>
 * 数据包结构：4字节数据内容长度+数据内容
 *
 * @author zkpursuit
 */
public class LengthDataDecoder extends LengthFieldBasedFrameDecoder {
    public LengthDataDecoder(int maxFrameLength) {
        super(maxFrameLength, 0, 4, 0, 4);
    }
}
