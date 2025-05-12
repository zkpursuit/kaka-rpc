package com.kaka.rpc.core;

import com.kaka.notice.Message;
import io.netty.channel.ChannelHandlerContext;

/**
 * 通信协议领域事件消息
 *
 * @author zkpursuit
 */
public class RpcMessage extends Message {

    ChannelHandlerContext ctx;
    Object client;
    boolean fromServer = false; //是否为服务器发送的消息

    public RpcMessage(Object what, Object data, ChannelHandlerContext ctx) {
        super(what, data);
        this.ctx = ctx;
        this.client = NettyCtxManager.getBindId(this.ctx);
    }

    public RpcMessage(Object what, Object data, Object client) {
        super(what, data);
        if (client != null) {
            if (client instanceof ChannelHandlerContext ctx) {
                this.ctx = ctx;
                Object ctxId = NettyCtxManager.getBindId(this.ctx);
                if (ctxId != null) {
                    this.client = String.valueOf(ctxId);
                }
            } else if (client instanceof String cs) {
                this.ctx = NettyCtxManager.instance().get(cs);
                this.client = cs;
            } else if (client instanceof NettyClient ntc) {
                this.ctx = ntc.getChannelHandlerContext();
                this.client = ntc;
                this.fromServer = true;
            }
        }
    }

    void setWhat(Object what) {
        this.what = what;
    }

    void setData(Object data) {
        this.body = data;
    }

    public ChannelHandlerContext getCtx() {
        return this.ctx;
    }

    public <T> T getClient() {
        return (T) this.client;
    }

    public boolean isFromServer() {
        return fromServer;
    }

    @Override
    public void reset() {
        super.reset();
        this.ctx = null;
        this.client = null;
    }

}
