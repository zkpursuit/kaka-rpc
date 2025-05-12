package com.test.server;

import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.rpc.RemotingServiceDetector;
import com.kaka.rpc.core.NettyServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 模拟多台服务器集群
 *
 * @author zkpursuit
 */
public class DemoMultiServer extends DemoSingleServer {

    public static void main(String[] args) {
        initLogback();

        Facade facade = FacadeFactory.getFacade();
        DemoMultiServer testServer = new DemoMultiServer();
        testServer.addDetector(new RemotingServiceDetector()); //如需使用 RemotingService 注解自动注册接口服务，则必须使用，否则请参看 RemotingServiceDetector 内部代码进行手动注册
        testServer.scan("com.test.server"); //扫描类包下的类进行领域事件处理器注册，或者RPC接口服务（请参看RemotingServiceDetector）注册
        facade.initThreadPool(Executors.newFixedThreadPool(6));

        multiServer();
    }

    // 模拟多台服务器集群
    static void multiServer() {
        for (int i = 7777; i <= 7780; i++) {
            final int port = i;
            new Thread(() -> {
                NettyServer socketServer = new NettyServer();
                int ioPoolSize = Runtime.getRuntime().availableProcessors() * 2;
                socketServer.start(new InetSocketAddress(port), ioPoolSize, 300, 7000, 3000, 0);
            }).start();
        }
    }

}
