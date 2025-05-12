package com.test.server;

import com.kaka.rpc.core.ErrorSender;
import com.kaka.rpc.core.NettyCtxManager;
import com.kaka.rpc.core.ProtocolPreHandler;
import com.kaka.rpc.core.RpcOpCode;
import com.kaka.util.IntSet;
import com.test.MyOpCode;
import io.netty.channel.ChannelHandlerContext;

/**
 * 通信协议前置处理，此处仅用于访问鉴权
 *
 * @author zkpursuit
 */
public class DemoProtocolPreHandler implements ProtocolPreHandler, ErrorSender {

    /**
     * 直接放行的数据协议号
     */
    private static final IntSet pass_opcode = new IntSet();

    static {
        pass_opcode.add(Integer.parseInt(MyOpCode.heart_beat));
        pass_opcode.add(Integer.parseInt(RpcOpCode.cs_rpc));
    }

    @Override
    public boolean handler(int opcode, Object param, ChannelHandlerContext ctx) throws Exception {
        if (param instanceof String rpcInvokeName) { // 此处为rpc调用
            if (rpcInvokeName.equals("login")) return true; // 放行客户端登录请求
            Object ctxId = NettyCtxManager.getBindId(ctx);
            if (!(ctxId instanceof Long)) { // 其它RPC调用需鉴权
                throw new SecurityException("鉴权失败，请先登录！"); // rpc调用需抛出异常，由rpc执行流程捕获处理
            }
            return true;
        }
        if (pass_opcode.contains(opcode)) return true;
        Object ctxId = NettyCtxManager.getBindId(ctx);
        if (!(ctxId instanceof Long)) {
            this.sendError(ctx, opcode, 1, 10001, "鉴权失败，请先登录！");
            return false;
        }
        return true;
    }
}
