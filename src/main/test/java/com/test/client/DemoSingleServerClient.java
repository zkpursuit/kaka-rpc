package com.test.client;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.kaka.Startup;
import com.kaka.rpc.core.IClient;
import com.kaka.rpc.core.NettyClient;
import com.kaka.rpc.RpcClient;
import com.kaka.util.ResourceUtils;
import com.test.DemoService;
import com.test.MyOpCode;
import com.test.server.DemoSingleServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class DemoSingleServerClient extends Startup {

    protected static void initLogback() {
        Logger logger = LoggerFactory.getLogger(DemoSingleServer.class);
        String path = ResourceUtils.getClassLoaderPath(DemoSingleServer.class);
        File file = new File(path + "/sys-config/logback.xml");
        InputStream is = null;
        if (!file.exists()) {
            is = ResourceUtils.getResourceAsStream("sys-config/logback.xml", DemoSingleServer.class);
            if (is == null) return;
        } else {
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(loggerContext);
        loggerContext.reset();
        try {
            joranConfigurator.doConfigure(is);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        initLogback();

        DemoSingleServerClient startup = new DemoSingleServerClient();
        startup.scan("com.test.client"); //扫描指定包下的所有类，处理自定义领域事件

        final CountDownLatch latch = new CountDownLatch(1);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 7777);
        NettyClient client = new NettyClient(address, 3, 7, 2, 2) {

            @Override
            protected void ping() {
                //发送自定义心跳包
                ChannelHandlerContext ctx = this.getChannelHandlerContext();
                if (ctx == null) return;
                ByteBuf buf = ctx.alloc().buffer(4); //标准包体结构 int协议号+协议内容
                buf.writeInt(Integer.parseInt(MyOpCode.heart_beat));
                ctx.writeAndFlush(buf);
//                //如下方式亦可，但服务器必须进行逻辑处理并返回数据，否则将造成client调用端数据积压，积压数据在缓存超时或调用超时时自动清除。
//                try {
//                    //异步回调方式发送心跳包
//                    this.execRemotingLogic(RpcCode.internal_cmd_heart_beat, 4, (res, e) -> {
//                        if (e != null) {
//                            return;
//                        }
//                        System.out.println("异步获取心跳数据>>> " + res);
//                    });
//                } catch (Exception ex) {
//                    logger.error(ex.getLocalizedMessage(), ex);
//                }
            }

            @Override
            protected void afterConnected() {
                this.execRemotingLogic("login", 3, (res, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }
                    if (res.equals(1)) {
                        System.out.println("登录成功");
                        latch.countDown();
                    }
                }, "user0", "123456");
            }

            @Override
            protected void afterDisconnect() {

            }

            @Override
            protected void onErrorCodeListener(int triggerErrorOpcode, int errLevel, int errCode, String errInfo) {

            }
        };
        client.connect();

        latch.await();
        test0(client);

        //String val = client.execRemotingLogic("demo_logic3");
        //System.out.println("同步获取远程结果>>> " + val);
    }

    protected static void test0(IClient client) throws Exception {
        //RPC远程领域事件处理，并同步获取执行结果，服务端请参看领域事件处理器 com.test.server.DemoLogic1Handler
        int result = client.execRemotingLogic("demo_logic1", 3, new Object[]{1, 2, 3});
        System.out.println("同步获取远程结果>>> " + result);
        //RPC远程领域事件处理，并异步回调获取结果，服务端请参看领域事件处理器 com.test.server.DemoLogic2Handler
        client.execRemotingLogic("demo_logic2", (res, e) -> {
            if (e != null) {
                //处理异常
                e.printStackTrace();
                return;
            }
            System.out.println("异步回调获取结果>>> " + res);
        }, 100);
        //RPC公共接口方法调用，服务端接口实现请参看 com.test.server.DemoServiceImpl
        DemoService testInterface = RpcClient.getRefInstance(DemoService.class, client);
        System.out.println("远程获取结果>>> " + testInterface.say("hello"));
        System.out.println("远程获取结果>>> " + testInterface.num(3));
        System.out.println("远程计算两数之和>>> " + testInterface.add(3, 7));
        System.out.println("远程计算两数之商>>> " + testInterface.divide(10f, 0.5f));
        System.out.println("远程计算两数之间所有数的和>>> " + testInterface.sum(1, 1000));
    }

    protected static void test1(IClient client) throws Exception {
        DemoService testInterface = RpcClient.getRefInstance(DemoService.class, client);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            testInterface.say("hello");
        }
        System.out.println("1000000次远程调用耗时>>> " + (System.currentTimeMillis() - start));
    }

}
