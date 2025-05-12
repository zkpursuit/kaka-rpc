package com.test.server;

import com.alibaba.fastjson.JSONObject;
import com.kaka.notice.annotation.Handler;
import com.kaka.rpc.core.RpcMessage;
import com.kaka.rpc.core.RpcMessageHandler;

/**
 * 领域事件处理器
 *
 * @author zkpursuit
 */
@Handler(cmd = "demo_logic2")
public class DemoLogic2Handler extends RpcMessageHandler {

    @Override
    public Object execute(RpcMessage msg) {
        Runtime rt = Runtime.getRuntime();
        String a = (rt.maxMemory() / 1024 / 1024) + "M";
        String b = (rt.totalMemory() / 1024 / 1024) + "M";
        String c = (rt.freeMemory() / 1024 / 1024) + "M";
        JSONObject json = new JSONObject();
        json.put("起始堆最大内存", a);
        json.put("起始堆初始内存", b);
        json.put("起始堆可用内存", c);
        return json.toString();
    }
}
