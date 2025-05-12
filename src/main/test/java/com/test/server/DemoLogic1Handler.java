package com.test.server;

import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;

/**
 * 领域事件处理器
 *
 * @author zkpursuit
 */
@Handler(cmd = "demo_logic1")
public class DemoLogic1Handler extends RpcMessageHandler {
    @Override
    public Object execute(RpcMessage msg) {
        Object[] params = (Object[]) msg.getBody(); //前端传入的参数
        return params[0];
    }
}
