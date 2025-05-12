package com.test.server;

import com.kaka.notice.Proxy;
import com.kaka.rpc.RemotingService;
import com.test.DemoService;

/**
 * rpc接口实现类
 */
@RemotingService(DemoService.class)
public class DemoServiceImpl extends Proxy implements DemoService {
    @Override
    public String say(String text) {
        return text;
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public long sum(int min, int max) {
        int sum = 0;
        for (int i = min; i <= max; i++) {
            sum += i;
        }
        return sum;
    }

    @Override
    public float divide(float a, float b) {
        return a / b;
    }

    @Override
    public int num(int val) {
        return val;
    }
}
