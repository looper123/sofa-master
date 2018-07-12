#### 介绍
SOFABolt 是蚂蚁金融服务集团开发的一套基于 Netty 实现的网络通信框架。

- 为了让 Java 程序员能将更多的精力放在基于网络通信的业务逻辑实现上，而不是过多的纠结于网络底层 NIO 的实现以及处理难以调试的网络问题，Netty 应运而生。
- 为了让中间件开发者能将更多的精力放在产品功能特性实现上，而不是重复地一遍遍制造通信框架的轮子，SOFABolt 应运而生。

Bolt 名字取自迪士尼动画-闪电狗，是一个基于 Netty 最佳实践的轻量、易用、高性能、易扩展的通信框架。 这些年我们在微服务与消息中间件在网络通信上解决过很多问题，积累了很多经验，并持续的进行着优化和完善，我们希望能把总结出的解决方案沉淀到 SOFABolt 这个基础组件里，让更多的使用网络通信的场景能够统一受益。 目前该产品已经运用在了蚂蚁中间件的微服务 (SOFARPC)、消息中心、分布式事务、分布式开关、以及配置中心等众多产品上。

#### 功能介绍
![framework](https://raw.githubusercontent.com/alipay/sofa-bolt/master/.middleware-common/intro.png)

#### SOFABolt基础功能
- 基础通信功能
    * 基于 Netty 高效的网络 IO 与线程模型运用
    * 连接管理(无锁建连，定时断连，自动重连)
    * 基础通信模型(oneway,sync,future,callback)
    * 超时控制
    * 批量解包和批量提交处理器
    * 心跳与IDLE事件处理器
- 协议框架
    * 命令与命令处理器
    * 编解码处理器
    * 心跳触发器
- 私有协议定制实现-RPC通信(protocol implementation)
    * RPC通信协议的设计
    * 灵活的反序列化时机控制
    * 请求处理超时FailFast机制
    * 用户请求处理器(UserProcessor)
    * 双工通信
    
#### 用法1 
将 SOFABolt 用作一个远程通信框架，使用者可以不用关心如何实现一个私有协议的细节，直接使用我们内置的 RPC 通信协议。可以非常简单的启动客户端与服务端，同时注册一个用户请求处理器，即可完成远程调用。同时，像连接管理、心跳等基础功能特性都默认可以使用。 当前支持的调用类型如下图所示：
![framework](https://raw.githubusercontent.com/alipay/sofa-bolt/master/.middleware-common/invoke_types.png)

#### 用法2
将 SOFABolt 用作一个协议框架，使用者可以复用基础的通信模型、协议包含的接口定义等基础功能。然后根据自己设计的私有协议自定义 Command 类型、Command 处理器、编解码处理器等。如下图所示，RPC 和消息的 Command 定义结构：
![framework](https://raw.githubusercontent.com/alipay/sofa-bolt/master/.middleware-common/msg_protocol.png)
  
### 基础功能
####1.1 实现用户请求处理器(UserProcessor)
SOFABOLT提供了两种用户请求处理器，SyncUserProcessor和AsyncUserProcessor,两个处理器的区别在于，前者需要在当前线程return形式返回处理结果；而
后者是通过AsyncContext,在当前线程或者异步线程中调用sendResponse方法返回处理结果。示例类：
- [同步请求处理器](./src/main/java/com/quark/sofa/processor/SynServerProcessor.java)
- [异步请求处理器](./src/main/java/com/quark/sofa/processor/AsynServerProcessor.java)

####1.2 实现连接事件处理器(ConnectionEventProcessor)
SOFABOLT提供了两种事件监听，建连事件(ConnectionEventType.CONNECT)和断连事件(ConnectionType.CLOSE)，用户可以创建自己的事件处理器，
并注册到客户端或者服务端，都可以监听到各自的建连与断连事件。示例类：
- [处理建连事件](./src/main/java/com/quark/sofa/processor/ConnectEventProcessor.java)
- [处理断连事件](./src/main/java/com/quark/sofa/processor/DisConnectEventProcessor.java)

####1.3 客户端与服务端初始化(RpcClient,RpcServer)
我们提供了一个RpcClient和RpcServer，经过简单的必要功能初始化，或者开关即可使用。示例如下：
- [客户端初始化](./src/main/java/com/quark/sofa/rpc/RpcClientDemoByMain.java)
- [服务端初始化](./src/main/java/com/quark/sofa/rpc/RpcServerDemoByMain.java)

####1.4 基础通信模型(4种)
- oneway调用 ![示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L69)
    * 当线程发起调用后，不关心调用结果，不做超时控制，只要请求已经发出，就完成本次调用。需要注意的是oneway调用不保证成功，并且发起方无法获取调用结果。
    因此可以用作定时通知或者重试的场景，调用过程可能会因为网络、机械等故障导致请求失败的。业务上需要对这些异常情况做处理后才能使用
- sync同步调用 ![示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L80)
    * 当线程发起调用后，需要在指定的超时时间内，等到响应结果，才算完成本次调用。如果超时时间内没有得到结果，那么会排除超时异常。这种嗲用模式最常用。
    注意要根据对端的处理能力，合理设置超时时间。
- Future调用 ![示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L103)
    * 当前线程发起调用，得到一个RpcResponseFutrue对象，当前线程可以继续执行下一次调用。可以在任意时刻，使用RpcRespnseFuture对象的get()方法
    来获取结果，如果响应已经回来，此时就马上得到结果；如果响应没有回来，则会阻塞住当前线程，知道响应回来，或者超时时间到。
- CallBack异步调用 ![示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L127)
        