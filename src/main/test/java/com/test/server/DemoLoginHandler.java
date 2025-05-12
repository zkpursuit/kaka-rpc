package com.test.server;

import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.NettyCtxManager;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;
import com.kaka.util.MathUtils;
import io.netty.channel.ChannelHandlerContext;

/**
 * 登录处理，请参看{@code com.test.server.DemoTestServer}中的{@code singleServer}方法
 *
 * @author zkpursuit
 */
@Handler(cmd = "login")
public class DemoLoginHandler extends RpcMessageHandler {
    @Override
    public Object execute(RpcMessage message) throws Throwable {
        ChannelHandlerContext ctx = message.getCtx();
        Object[] params = (Object[]) message.getBody();
        String userName = String.valueOf(params[0]);
        String password = String.valueOf(params[1]);
        long uid = MathUtils.random(10000000, 99999999); //假设用户名和密码验证通过后生成用户ID
        NettyCtxManager.instance().bind(ctx, uid); //为信道绑定用户ID
        return 1;
    }
}
