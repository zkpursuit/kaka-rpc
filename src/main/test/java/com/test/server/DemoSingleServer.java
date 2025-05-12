package com.test.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.kaka.Startup;
import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.rpc.core.NettyServer;
import com.kaka.rpc.RemotingServiceDetector;
import com.kaka.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 测试服务端
 *
 * @author zkpursuit
 */
public class DemoSingleServer extends Startup {

    static void initLogback() {
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

    public static void main(String[] args) {
        initLogback();

        Facade facade = FacadeFactory.getFacade();
        DemoSingleServer testServer = new DemoSingleServer();
        testServer.addDetector(new RemotingServiceDetector()); //如需使用 RemotingService 注解自动注册接口服务，则必须使用，否则请参看 RemotingServiceDetector 内部代码进行手动注册
        testServer.scan("com.test.server"); //扫描类包下的类进行领域事件处理器注册，或者RPC接口服务（请参看RemotingServiceDetector）注册
        facade.initThreadPool(Executors.newFixedThreadPool(6));

        singleServer();
    }

    // 单体服务
    private static void singleServer() {
        NettyServer socketServer = new NettyServer(new DemoProtocolPreHandler()); // DemoProtocolPreHandler 用于登录鉴权
        int ioPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        socketServer.start(new InetSocketAddress(7777), ioPoolSize, 300, 7000, 3000, 0);
    }

}
