package com.kaka.rpc.core;

import com.kaka.util.ConsistentHash;
import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * 集群客户端 <br>
 * 集群客户端连接多个服务器，并使用一致性Hash算法进行负载均衡 <br>
 *
 * @author zkpursuit
 */
abstract public class ClusterClient implements IClient {

    /**
     * 集群客户端连接的单元
     */
    public static class Unit {
        private final InetSocketAddress address;
        private final NettyClient client;

        public Unit(InetSocketAddress address) {
            this.address = address;
            this.client = null;
        }

        public Unit(NettyClient client) {
            this.address = client.getAddress();
            this.client = client;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public NettyClient getClient() {
            return client;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Unit that = (Unit) o;
            return Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(address);
        }

        @Override
        public String toString() {
            return address.toString();
        }
    }

    /**
     * 一致性Hash
     */
    protected final ConsistentHash<Unit> ch;

    /**
     * 集群客户端
     *
     * @param addresses        连接的服务器地址
     * @param numberOfReplicas 负载均衡系数
     */
    public ClusterClient(List<InetSocketAddress> addresses, int numberOfReplicas) {
        this.ch = new ConsistentHash<>(numberOfReplicas);
        for (InetSocketAddress address : addresses) {
            NettyClient ntc = this.createClient(address);
            ntc.connect();
        }
    }

    /**
     * 添加一个集群单元
     *
     * @param unit 集群单元
     */
    public void addUnit(Unit unit) {
        ch.addNode(unit);
    }

    /**
     * 移除一个集群单元
     *
     * @param unit 集群单元
     */
    public void removeUnit(Unit unit) {
        ch.removeNode(unit);
    }

    /**
     * 根据负载均衡算法获取一个集群单元
     *
     * @param key 负载均衡算法的key
     * @return 集群单元
     */
    protected Unit getUnit(String key) {
        return ch.getNode(key);
    }

    /**
     * 创建一个TCP客户端
     *
     * @param address 服务器地址
     * @return TCP客户端
     */
    abstract protected NettyClient createClient(InetSocketAddress address);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendAndFlush(ByteBuf data) {
        if (data.readableBytes() >= 4) {
            String cmd = String.valueOf(data.getInt(0));
            Unit unit = this.getUnit(cmd);
            if (unit != null) {
                NettyClient client = unit.getClient();
                if (client != null) {
                    return client.sendAndFlush(data);
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Object> execRemotingLogic0(String cmd, int timeoutSeconds, Object[] params) throws Exception {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                return client.execRemotingLogic0(cmd, timeoutSeconds, params);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Object> execRemotingLogic0(String cmd, Object[] params) throws Exception {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                return client.execRemotingLogic0(cmd, params);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execRemotingLogic(String cmd, int timeoutSeconds, Object[] params) throws Exception {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                return client.execRemotingLogic(cmd, timeoutSeconds, params);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execRemotingLogic(String cmd, Object... params) throws Exception {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                return client.execRemotingLogic(cmd, params);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execRemotingLogic(String cmd, int timeoutSeconds, BiConsumer<Object, ? super Throwable> action, Object... params) {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                client.execRemotingLogic(cmd, timeoutSeconds, action, params);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execRemotingLogic(String cmd, BiConsumer<Object, ? super Throwable> action, Object... params) {
        Unit unit = this.getUnit(cmd);
        if (unit != null) {
            NettyClient client = unit.getClient();
            if (client != null) {
                client.execRemotingLogic(cmd, action, params);
            }
        }
    }
}
