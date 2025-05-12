package com.kaka.rpc.core;

import com.kaka.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

/**
 * 错误发送者
 *
 * @author zkpursuit
 */
public interface ErrorSender {

    /**
     * 发送错误信息
     *
     * @param ctx                信道上下文
     * @param triggerErrorOpcode 触发错误的操作码
     * @param errLevel           错误等级
     * @param errCode            错误码
     * @param errInfo            错误信息
     * @param errInfoParams      错误信息参数，该参数将替换errInfo中的“{}”通配符
     */
    default void sendError(ChannelHandlerContext ctx, int triggerErrorOpcode, int errLevel, int errCode, String errInfo, Object... errInfoParams) {
        if (errInfoParams != null && errInfoParams.length > 0) {
            errInfo = StringUtils.format(errInfo, errInfoParams);
        }
        byte[] errBytes = errInfo.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer(13 + errBytes.length);
        buf.writeInt(triggerErrorOpcode);
        buf.writeByte(errLevel);
        buf.writeInt(errCode);
        buf.writeInt(errBytes.length);
        buf.writeBytes(errBytes);
        ctx.writeAndFlush(buf);
    }

}
