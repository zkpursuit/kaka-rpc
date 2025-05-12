package com.kaka.rpc.core;

/**
 * 连接状态码
 *
 * @author zkpursuit
 */
public class NettyStateCode {
    /**
     * msg.getBody() == [ChannelHandlerContext, Throwable]
     */
    public static final String CHANNEL_EXCEPTION = "channel_exception";
    /**
     * msg.getBody() == ChannelHandlerContext
     */
    public static final String CHANNEL_ACTIVE = "channel_active";
    /**
     * msg.getBody() == ChannelHandlerContext
     */
    public static final String CHANNEL_REMOVE = "channel_remove";
    /**
     * msg.getBody() == ChannelHandlerContext
     */
    public static final String CHANNEL_READER_IDLE = "channel_reader_idle";
    /**
     * msg.getBody() == ChannelHandlerContext
     */
    public static final String CHANNEL_WRITER_IDLE = "channel_writer_idle";
    /**
     * msg.getBody() == ChannelHandlerContext
     */
    public static final String CHANNEL_RW_IDLE = "channel_rw_idle";

}
