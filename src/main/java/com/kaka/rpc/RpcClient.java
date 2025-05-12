package com.kaka.rpc;

import com.kaka.rpc.core.IClient;
import com.kaka.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC客户端 <br>
 * <pre>
 *     {@code
 *        NettyClient client = ...
 *        DemoService demoService = RpcClient.getRefInstance(DemoService.class, client);
 *        demoService.say("hello rpc world");
 *     }
 * </pre>
 *
 * @author zkpursuit
 */
final public class RpcClient {

    private final static Map<Class<?>, Long> interfaceIdMap = new ConcurrentHashMap<>();
    private final static Map<Class<?>, Object> interfaceInstanceMap = new ConcurrentHashMap<>();
    private final static Map<Class<?>, Map<Method, Long>> interfaceMethodMap = new ConcurrentHashMap<>();

    /**
     * 获取远端接口代理
     *
     * @param interfaceClass 远端需要实现的接口
     * @param client         tcp客户端
     * @param <T>            远端接口泛型限定
     * @return 远端接口代理
     */
    public static <T> T getRefInstance(Class<T> interfaceClass, IClient client) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("interfaceClass must be an interface");
        }
        final Long interfaceId = interfaceIdMap.computeIfAbsent(interfaceClass, k -> StringUtils.toNumber(interfaceClass.getName()));
        final Map<Method, Long> map = interfaceMethodMap.computeIfAbsent(interfaceClass, k -> getAllMethods(interfaceClass));
        Object instance = interfaceInstanceMap.computeIfAbsent(interfaceClass, k -> Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, (proxy, method, args) -> {
            String rpcCmd = "rpc:" + interfaceClass.getName() + ":" + method.getName() + ":" + map.get(method) + ":" + interfaceId;
            return client.execRemotingLogic(rpcCmd, 3, args);
        }));
        return (T) instance;
    }

    /**
     * 获取接口所有方法，并映射为唯一标识ID，此ID为方法完全限定名的MD5转换为Long，相对有限的接口方法，重复碰撞率可忽略
     *
     * @param interfaceClass 接口类
     */
    private static Map<Method, Long> getAllMethods(Class<?> interfaceClass) {
        Map<Method, Long> allMethods = new HashMap<>();
        Class<?>[] interfaces = interfaceClass.getInterfaces();
        for (Class<?> clazz : interfaces) {
            allMethods.putAll(getAllMethods(clazz));
        }
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodStr = method.toGenericString().replace("public ", "").replace("abstract", "").trim();
            allMethods.put(method, StringUtils.toNumber(methodStr));
        }
        return allMethods;
    }

}
