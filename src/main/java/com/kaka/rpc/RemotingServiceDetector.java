package com.kaka.rpc;

import com.kaka.notice.Facade;
import com.kaka.notice.FacadeFactory;
import com.kaka.notice.Message;
import com.kaka.notice.Proxy;
import com.kaka.notice.detector.IDetector;
import com.kaka.notice.detector.ProxyDetector;
import com.kaka.util.StringUtils;

/**
 * RPC接口服务自动扫描处理器 <br>
 * 如需使用 RemotingService 注解自动注册接口服务，则必须在{@link com.kaka.Startup}子类中调用
 * <pre>
 *     {@code addDetector(new RemotingServiceDetector())}
 * </pre>
 * <p>
 * 否则请参看{@code discern}方法进行手动注册
 *
 * @author zkpursuit
 */
public class RemotingServiceDetector implements IDetector {

    public String name() {
        return "RemotingServiceDetector";
    }

    public boolean discern(Class<?> cls) {
        if (!Proxy.class.isAssignableFrom(cls)) {
            return false;
        }
        RemotingService anno = cls.getAnnotation(RemotingService.class);
        if (anno == null) {
            return false;
        }
        Class<?> itfClass = anno.value();
        Facade facade = anno.context().isEmpty() ? FacadeFactory.getFacade() : FacadeFactory.getFacade(anno.context());
        Long itfId = StringUtils.toNumber(itfClass.getName());
        Proxy proxy = facade.registerProxy((Class<? extends Proxy>) cls, itfClass.getName(), String.valueOf(itfId));
        if (facade.hasCommand("print_log")) {
            facade.sendMessage(new Message("print_log", new Object[]{ProxyDetector.class, new Object[]{proxy.getName(), cls}}));
        }
        return true;
    }
}
