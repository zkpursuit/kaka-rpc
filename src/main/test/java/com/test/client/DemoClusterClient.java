package com.test.client;

import com.kaka.rpc.core.ClusterClient;
import com.kaka.rpc.core.NettyClient;
import com.test.MyOpCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DemoClusterClient extends DemoSingleServerClient {

    public static void main(String[] args) throws Exception {
        initLogback();

        DemoClusterClient startup = new DemoClusterClient();
        startup.scan("com.test.client"); //扫描指定包下的所有类，处理自定义领域事件

        final int numberOfReplicas = 64; //均衡系数，调整此值，可以调整集群节点的负载均衡平衡度
        final List<InetSocketAddress> addresses = new ArrayList<>() {{
            add(new InetSocketAddress("127.0.0.1", 7777));
            add(new InetSocketAddress("127.0.0.1", 7778));
            add(new InetSocketAddress("127.0.0.1", 7779));
            add(new InetSocketAddress("127.0.0.1", 7780));
        }};
        final CountDownLatch latch = new CountDownLatch(addresses.size());
        final ClusterClient clusterClient = new ClusterClient(addresses, numberOfReplicas) {

            @Override
            protected NettyClient createClient(InetSocketAddress address) {
                return new NettyClient(address, 3, 7, 2, 2) {
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
                        addUnit(new Unit(this));
                        latch.countDown();
                    }

                    @Override
                    protected void afterDisconnect() {
                        removeUnit(new Unit(this.getAddress()));
                    }

                    @Override
                    protected void onErrorCodeListener(int triggerErrorOpcode, int errLevel, int errCode, String errInfo) {

                    }
                };
            }
        };
        latch.await();
        test0(clusterClient);
    }
}