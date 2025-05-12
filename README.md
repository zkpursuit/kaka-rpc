<h1 align="center" style="text-align:center;">
  kaka-rpc
</h1>
<p align="center" style="text-align:center;">
	<strong>基于TCP与领域事件的RPC框架</strong>
</p>

<p align="center" style="text-align:center;">
	<img src="https://img.shields.io/badge/QQ交流群-801241310-orange" alt="help"/>
    <img src="https://img.shields.io/badge/答疑交流（微信）-zkpursuit-blue" alt="help"/>
</p>

<hr />

#### 介绍

基于Netty TCP与kaka-core领域事件的轻量级RPC框架，之所谓轻量，相比其它使用各种繁杂设计模式的框架，本框架仅10多个类实现，可谓是极简编码。

* 基础通信
    * 自定义心跳协议或基于rpc调用的心跳处理
    * 断线重连
    * 同步、future异步、callback回调多种rpc调用模式
    * 超时控制
    * 自定义数据协议扩展领域事件
* 事件驱动
    * 遵循一切皆事件的领域模型
    * 通信协议即为事件
* 集群部署
    * 方案一：Nginx负载均衡
    * 方案二：自定义负载均衡，基于一致性哈希算法，亦可使用心跳轮询基于连接量的动态权重负载均衡

### 运行时要求jdk21+

#### 安装

```xml
<dependency>
    <groupId>io.github.zkpursuit</groupId>
    <artifactId>kaka-rpc</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 使用说明

具体示例可参看源码test目录

```java
final CountDownLatch latch = new CountDownLatch(1);
InetSocketAddress address = new InetSocketAddress("127.0.0.1", 7777);
NettyTcpClient client = new NettyTcpClient(address, 3, 7, 2, 2) {

    @Override
    protected void ping() {
        //发送自定义心跳包
        ChannelHandlerContext ctx = this.getChannelHandlerContext();
        if (ctx == null) return;
        ByteBuf buf = ctx.alloc().buffer(4); //标准包体结构 int协议号+协议内容
        buf.writeInt(Integer.parseInt(OpCode.internal_cmd_heart_beat));
        ctx.writeAndFlush(buf);
// //如下方式亦可，但服务器必须进行逻辑处理并返回数据，否则将造成client调用端数据积压，且积压数据仅能通过超时自动清除。
// try {
//                    //异步回调方式发送心跳包
//                    this.execRemotingLogic(OpCode.internal_cmd_heart_beat, 4, (res, e) -> {
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
        latch.countDown();
    }

    @Override
    protected void afterDisconnect() {

    }
};
client.connect();
latch.await();

//RPC远程领域事件处理，并同步获取执行结果，服务端请参看领域事件处理器 com.test.server.DemoLogic1Handler
int result = client.execRemotingLogic("demo_logic1", 3, new Object[]{1, 2, 3});
System.out.println("同步获取远程结果>>> "+result);
//RPC远程领域事件处理，并异步回调获取结果，服务端请参看领域事件处理器 com.test.server.DemoLogic2Handler
client.execRemotingLogic("demo_logic2",(res, e) ->{
    if(e !=null){
        //处理异常
        return;
    }
    System.out.println("异步回调获取结果>>> "+res);
},100);
//RPC公共接口方法调用，服务端接口实现请参看 com.test.server.DemoServiceImpl
DemoService testInterface = RpcClient.getRefInstance(DemoService.class, client);
System.out.println("远程获取结果>>> "+testInterface.say("hello"));
System.out.println("远程获取结果>>> "+testInterface.num(3));
System.out.println("远程计算两数之和>>> "+testInterface.calc(3, 7));
System.out.println("远程计算两数之间所有数的和>>> "+testInterface.sum(1, 1000));
```