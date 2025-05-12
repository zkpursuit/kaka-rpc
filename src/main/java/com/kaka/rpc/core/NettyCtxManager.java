package com.kaka.rpc.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端连接管理器
 *
 * @author zkpursuit
 */
public final class NettyCtxManager {
    /**
     * socket通信channel所属的服务ID
     */
    public static final AttributeKey<Long> server_id = AttributeKey.valueOf("srvId");
    /**
     * socket通信channel的id，对应一个客户端
     */
    public static final AttributeKey<Object> bind_id = AttributeKey.valueOf("id");

    private final Map<Long, Map<Object, ChannelHandlerContext>> server_client_map = new ConcurrentHashMap<>();
    private static final NettyCtxManager instance = new NettyCtxManager();

    /**
     * 构造方法，不允许外部实例化
     */
    private NettyCtxManager() {
    }

    public static NettyCtxManager instance() {
        return instance;
    }

    public static Object getBindId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(bind_id).get();
    }

    public static Long getServerId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(server_id).get();
    }

    public static String getRemoteAddress(ChannelHandlerContext ctx) {
        InetSocketAddress inSocket = (InetSocketAddress) ctx.channel().remoteAddress();
        return inSocket.getAddress().getHostAddress();
    }

    /**
     * 移除客户端连接
     *
     * @param clientId 客户端连接唯一标识
     */
    private void remove(final Object clientId) {
        Iterator<Map.Entry<Long, Map<Object, ChannelHandlerContext>>> iterator = server_client_map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Map<Object, ChannelHandlerContext>> entry = iterator.next();
            Map<Object, ChannelHandlerContext> cMap = entry.getValue();
            ChannelHandlerContext ctx = cMap.remove(clientId);
            if (cMap.isEmpty()) {
                iterator.remove();
            }
            if (ctx != null) {
                ctx.channel().attr(bind_id).set(null);
            }
        }
    }

    /**
     * 添加客户端连接
     *
     * @param ctx      客户端连接管道
     * @param clientId 客户端连接唯一标识
     */
    public void bind(ChannelHandlerContext ctx, Object clientId) {
        Object id = NettyCtxManager.getBindId(ctx);
        if (clientId.equals(id)) return;
        remove(clientId);
        Long serverId = ctx.channel().attr(server_id).get();
        Map<Object, ChannelHandlerContext> cMap = server_client_map.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        cMap.put(clientId, ctx);
        ctx.channel().attr(bind_id).set(clientId);
    }

    /**
     * 获取客户端连接管道
     *
     * @param clientId 客户端连接唯一标识
     * @return 客户端连接管道
     */
    public ChannelHandlerContext get(Object clientId) {
        return server_client_map.values().stream().filter(cMap -> cMap.containsKey(clientId)).map(cMap -> cMap.get(clientId)).findFirst().orElse(null);
    }

    /**
     * 移除客户端连接
     *
     * @param ctx 客户端连接管道
     * @return socket信道绑定的ID
     */
    public Object remove(ChannelHandlerContext ctx) {
        Object clientId = NettyCtxManager.getBindId(ctx);
        if (clientId != null) {
            ChannelHandlerContext tempCtx = NettyCtxManager.instance.get(clientId);
            if (tempCtx == null) {
                this.remove(clientId);
            } else {
                String tempCtxId = tempCtx.channel().id().asLongText();
                String ctxId = ctx.channel().id().asLongText();
                if (ctxId.equals(tempCtxId)) {
                    this.remove(clientId);
                }
            }
            return clientId;
        }
        return null;
    }

    /**
     * 客户端总数量
     */
    public int getClientCount(Long serverId) {
        Map<Object, ChannelHandlerContext> cMap = server_client_map.get(serverId);
        if (cMap == null) return 0;
        return cMap.size();
    }

    /**
     * 客户端总数量
     */
    public int getClientCount() {
        return server_client_map.values().stream().mapToInt(Map::size).sum();
    }

}