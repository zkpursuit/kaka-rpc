package com.test.client;

import com.kaka.rpc.core.ClusterClient;
import com.kaka.rpc.core.NettyClient;
import com.kaka.util.ConsistentHash;
import com.test.MyOpCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DemoMultiServerClient extends DemoSingleServerClient {

    public static void main(String[] args) throws Exception {
        initLogback();

        DemoMultiServerClient startup = new DemoMultiServerClient();
        startup.scan("com.test.client"); //扫描指定包下的所有类，处理自定义领域事件

        final int numberOfReplicas = 5; //均衡系数
        final List<InetSocketAddress> addresses = new ArrayList<>() {{
            add(new InetSocketAddress("127.0.0.1", 7777));
            add(new InetSocketAddress("127.0.0.1", 7778));
            add(new InetSocketAddress("127.0.0.1", 7779));
            add(new InetSocketAddress("127.0.0.1", 7780));
        }};

        final ConsistentHash<ClusterClient.Unit> ch = new ConsistentHash<>(numberOfReplicas);
        final CountDownLatch latch = new CountDownLatch(addresses.size());
        for (InetSocketAddress address : addresses) {
            NettyClient ntc = new NettyClient(address, 3, 7, 2, 2) {
                @Override
                protected void ping() {
                    //发送自定义心跳包
                    ChannelHandlerContext ctx = this.getChannelHandlerContext();
                    if (ctx == null) return;
                    ByteBuf buf = ctx.alloc().buffer(4); //标准包体结构 int协议号+协议内容
                    buf.writeInt(Integer.parseInt(MyOpCode.heart_beat));
                    ctx.writeAndFlush(buf);
//                    //如下方式亦可，但服务器必须进行逻辑处理并返回数据，否则将造成client调用端数据积压，积压数据在缓存超时或调用超时时自动清除。
//                    try {
//                        //异步回调方式发送心跳包
//                        this.execRemotingLogic(RpcCode.internal_cmd_heart_beat, 4, (res, e) -> {
//                            if (e != null) {
//                                return;
//                            }
//                            System.out.println("异步获取心跳数据>>> " + res);
//                        });
//                    } catch (Exception ex) {
//                        logger.error(ex.getLocalizedMessage(), ex);
//                    }
                }

                @Override
                protected void afterConnected() {
                    ch.addNode(new ClusterClient.Unit(this));
                    latch.countDown();
                }

                @Override
                protected void afterDisconnect() {
                    ch.removeNode(new ClusterClient.Unit(this.getAddress()));
                }

                @Override
                protected void onErrorCodeListener(int triggerErrorOpcode, int errLevel, int errCode, String errInfo) {

                }
            };
            ntc.connect();
        }
        latch.await();

        testRandomServer(ch);
    }

    /**
     * 测试随机连接到某个RPC服务器
     *
     * @param ch 服务器集群连接池
     */
    private static void testRandomServer(final ConsistentHash<ClusterClient.Unit> ch) throws Exception {
        ClusterClient.Unit unit = ch.getNode(); //当服务集群全部不可用时，此处获得null
        NettyClient client = unit.getClient();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("当前客户端连接的服务：" + client.getAddress() + "\n");
        test0(client);
    }

    /**
     * 测试连接到固定RPC服务器
     *
     * @param ch 服务器集群连接池
     */
    private static void testFixedServer(final ConsistentHash<ClusterClient.Unit> ch) throws Exception {
        ClusterClient.Unit unit = ch.getNode("abcd"); //当服务集群全部不可用时，此处获得null
        NettyClient client = unit.getClient();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("当前客户端连接的服务：" + client.getAddress() + "\n");
        test0(client);
    }

}
