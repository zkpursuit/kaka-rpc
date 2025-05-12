package com.kaka.rpc.core;

import com.kaka.notice.Facade;
import com.kaka.notice.IResult;
import com.kaka.notice.Proxy;
import com.kaka.notice.SyncResult;
import com.kaka.util.JdkSerializer;
import com.kaka.util.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC相关的领域事件处理
 *
 * @author zkpursuit
 */
public class RpcInvokeHandler extends RpcMessageHandler {

    public static Serializer<Object> serializer = new JdkSerializer();
    static int opcode = Integer.parseInt(RpcOpCode.cs_rpc);
    private final static Map<Long, Class<?>> interfaceClassMap = new ConcurrentHashMap<>(); //类名转换后的ID->类
    private final static Map<Class<?>, Map<Long, Method>> interfaceMethodMap = new ConcurrentHashMap<>(); //类->(方法完全限定名转后后的ID->方法)

    @Override
    public Object execute(RpcMessage rpcMessage) {
        ByteBuf msg = (ByteBuf) rpcMessage.getBody();
        short idLen = msg.readShort();
        byte[] idBytes = new byte[idLen];
        msg.readBytes(idBytes);
        short cmdLen = msg.readShort();
        byte[] cmdBytes = new byte[cmdLen];
        msg.readBytes(cmdBytes);
        String cmd = new String(cmdBytes, StandardCharsets.UTF_8);

        Facade facade = this.getFacade();
        ChannelHandlerContext ctx = rpcMessage.getCtx();
        ByteBuf sendBuf = ctx.alloc().buffer(100);
        sendBuf.writeInt(NettyClient.cmd_sc_sync_result);
        sendBuf.writeShort(idBytes.length);
        sendBuf.writeBytes(idBytes);
        sendBuf.writeShort(cmdLen);
        sendBuf.writeBytes(cmdBytes);

        if (NettyServer.protocolPreHandler != null) {
            try {
                boolean retVal = NettyServer.protocolPreHandler.handler(opcode, cmd, ctx);
                if (!retVal) return null;
            } catch (Exception e) {
                Utils.writeException(sendBuf, e, serializer);
                ctx.writeAndFlush(sendBuf);
                return null;
            }
        }

        int paramNum = msg.readShort();
        Exception paramEx = null;
        Object[] params = null;
        if (paramNum > 0) {
            params = new Object[paramNum];
            try {
                for (int i = 0; i < paramNum; i++) {
                    params[i] = Utils.readValue(msg, serializer);
                }
            } catch (Exception e) {
                paramEx = e;
            }
        }

        if (paramEx != null) {
            Utils.writeException(sendBuf, paramEx, serializer);
        } else if (cmd.startsWith("rpc:")) {
            final String rpcPath = cmd.substring("rpc:".length());
            final String[] parts = rpcPath.split(":");
            final String interfaceName = parts[0];
            final String methodName = parts[1];
            final String interfaceIdStr = parts.length > 3 ? (parts[3].isEmpty() || parts[3].isBlank() || "null".equals(parts[3]) ? null : parts[3]) : null;
            final Long interfaceId = interfaceIdStr != null ? Long.valueOf(interfaceIdStr) : null;
            final Long methodId = parts.length > 2 ? (parts[2].isEmpty() || parts[2].isBlank() || "null".equals(parts[2]) ? null : Long.parseLong(parts[2])) : null;
            try {
                Class<?> clazz = getInterfaceClass(interfaceName, interfaceId);
                Proxy proxy = this.retrieveProxy(interfaceIdStr == null ? clazz.getName() : interfaceIdStr);
                if (proxy != null) {
                    Map<Long, Method> methodMap = interfaceMethodMap.computeIfAbsent(clazz, k -> Utils.getAllMethods(clazz));
                    Method method = methodMap.get(methodId);
                    if (method == null) {
                        method = Utils.findMethod(proxy.getClass(), methodName, params);
                    }
                    if (method == null) {
                        Utils.writeException(sendBuf, new IllegalAccessException("未匹配到合适的方法" + methodName), serializer);
                    } else {
                        Utils.writeResult(sendBuf, method.invoke(proxy, params), serializer);
                    }
                } else {
                    Utils.writeException(sendBuf, new ClassNotFoundException("未找到接口对应的实现类"), serializer);
                }
            } catch (Throwable e) {
                Utils.writeException(sendBuf, e, serializer);
            }
        } else if (!facade.hasCommand(cmd)) {
            Utils.writeException(sendBuf, "未找到远程执行处理器：" + cmd, serializer);
        } else {
            try {
                RpcMessage message = new RpcMessage(cmd, params, ctx);
                IResult<Object> result = message.setResult("return", new SyncResult<>());
                this.sendMessage(message);
                Utils.writeResult(sendBuf, result.get(), serializer);
            } catch (Throwable ex) {
                Utils.writeException(sendBuf, ex, serializer);
            }
        }
        ctx.writeAndFlush(sendBuf);
        return null;
    }

    private Class<?> getInterfaceClass(String className, Long classId) throws ClassNotFoundException {
        Class<?> clazz = classId != null && interfaceClassMap.containsKey(classId) ? interfaceClassMap.get(classId) : null;
        if (clazz == null) {
            clazz = Class.forName(className);
            interfaceClassMap.put(classId, clazz);
        }
        return clazz;
    }
}
