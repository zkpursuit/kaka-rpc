package com.test.server;

import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;

@Handler(cmd = "demo_logic3")
public class DemoLogic3Handler extends RpcMessageHandler {
    @Override
    public Object execute(RpcMessage message) {
        int a = 1 / 0;
        //Thread.sleep(6000);
        return "success";
    }
}
