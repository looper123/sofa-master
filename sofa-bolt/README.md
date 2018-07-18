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
#### 1.1 实现用户请求处理器(UserProcessor)
SOFABOLT提供了两种用户请求处理器，SyncUserProcessor和AsyncUserProcessor,两个处理器的区别在于，前者需要在当前线程return形式返回处理结果；而
后者是通过AsyncContext,在当前线程或者异步线程中调用sendResponse方法返回处理结果。示例类：
- [同步请求处理器](./src/main/java/com/quark/sofa/processor/SynServerProcessor.java)
- [异步请求处理器](./src/main/java/com/quark/sofa/processor/AsynServerProcessor.java)

#### 1.2 实现连接事件处理器(ConnectionEventProcessor)
SOFABOLT提供了两种事件监听，建连事件(ConnectionEventType.CONNECT)和断连事件(ConnectionType.CLOSE)，用户可以创建自己的事件处理器，
并注册到客户端或者服务端，都可以监听到各自的建连与断连事件。示例类：
- [处理建连事件](./src/main/java/com/quark/sofa/processor/ConnectEventProcessor.java)
- [处理断连事件](./src/main/java/com/quark/sofa/processor/DisConnectEventProcessor.java)

#### 1.3 客户端与服务端初始化(RpcClient,RpcServer)
我们提供了一个RpcClient和RpcServer，经过简单的必要功能初始化，或者开关即可使用。示例如下：
- [客户端初始化](./src/main/java/com/quark/sofa/rpc/RpcClientDemoByMain.java)
- [服务端初始化](./src/main/java/com/quark/sofa/rpc/RpcServerDemoByMain.java)

#### 1.4 基础通信模型(4种)
- oneway调用 [示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L62)
    * 当线程发起调用后，不关心调用结果，不做超时控制，只要请求已经发出，就完成本次调用。需要注意的是oneway调用不保证成功，并且发起方无法获取调用结果。
    因此可以用作定时通知或者重试的场景，调用过程可能会因为网络、机械等故障导致请求失败的。业务上需要对这些异常情况做处理后才能使用
- sync同步调用 [示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L80)
    * 当线程发起调用后，需要在指定的超时时间内，等到响应结果，才算完成本次调用。如果超时时间内没有得到结果，那么会排除超时异常。这种嗲用模式最常用。
    注意要根据对端的处理能力，合理设置超时时间。
- Future调用 [示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L103)
    * 当前线程发起调用，得到一个RpcResponseFutrue对象，当前线程可以继续执行下一次调用。可以在任意时刻，使用RpcRespnseFuture对象的get()方法
    来获取结果，如果响应已经回来，此时就马上得到结果；如果响应没有回来，则会阻塞住当前线程，知道响应回来，或者超时时间到。
- CallBack异步调用 [示例](./src/main/java/com/quark/sofa/communication/ModelTest.java#L127)
    * 当前线程发起调用，则本次调用马上结束，马上可以执行下一次调用。发起调用时需要注册一个回调，该回调需要分配一个异步线程池。待响应回来后，会在回调
    的异步线程池，来执行回调逻辑。
    
#### 1.5 日志打印
SOFABOLT只依赖SELF4J。同时提供了log4j、log4j2、logback三种日志模板，使用者只需要在运行时依赖某一种日志实现，我们依赖的sofa-common-tools组件
，会在运行时动态感知是哪一种日志实现，同时加载正确的日志模板，进行打印。日志会打印在~/logs/bolt/目录下，日志类型有：
- common-default.log: 默认日志，打印一些客户端、服务器启动、关闭等通信过程的普通日志
- common-error.log: 异常日志，框架运行过程中出现的异常
- connection-event.log:连接事件日志
- remoting-rpc.log: RPC协议相关的日志

### 2 进阶功能
#### 2.1 请求上下文
在调用过程中，我们还提供了带InvokeContext的接口，并一路传递下去，可以在自定义序列化器，用户请求处理器中获得。我们分为两种场景来使用请求上下文：
- 客户端：用户可以设置一些针对本次请求生效的参数，比如序列化类型，是否开启crc机制。同时可以从上下文中获取建连耗时，连接信息等。
- 服务端：用户可以从用户请求处理器中获得请求到达后的排队耗时，连接信息等
- 注意：客户端与服务端的上下文是独立的，即客户端设置的上下文只在客户端可见，对服务端不可见；反之同理    
[示例](./src/main/java/com/quark/sofa/extra/RequestContext.java#L74)

#### 2.2 双工通信
除了服务端可以注册用户请求处理器，我们的客户端也可以注册用户请求处理器。此时，服务端就可以发起对客户端的调用，也可以使用上面[1.4](#1.4-基础通信模型(4种))
- 2.2.1 使用[Connection对象的双工通信](./src/main/java/com/quark/sofa/extra/DuplexCommunication.java#L72)，注意使用Connection对象的双工通信，服务端需要通过事件监听处理器或者用户请求处理器，自己保存好Connection对象。
- 2.2.2 使用[Address的双工通信](./src/main/java/com/quark/sofa/extra/DuplexCommunication.java#L91),注意使用Address方式的双工通信，需要在初始化RpcServer时，打开manageConnection开关，表示服务端会根据客户端发起
建连，维护一份地址与连接的映射关系。默认不需要双工通信的时候，这个功能时关闭的。

#### 2.3 建立多连接与连接预热
通常来说，点对点的直连通信，客户端和服务端，一个IP一个连接对象就足够了。不管是吞吐能力还是并发读，都能满足一般业务的通信需求。而一些场景，比如不是点对点直连通信，
而是经过LVS VIP,或者F5设备的连接，此时，为了负载均衡和容错，会针对一个URL增加如下参数`127.0.0.1:12200?_CONNECTIONNUM=30&_CONNECTIONWARMUP=true`，
来表示针对这个IP地址，需要建立30个连接，同时需要预热连接。其中预热与不预热的区别是：
- 预热：即第一次调用(比如Sync同步调用)，就建立30个连接
- 不预热：每一次调用，创建一个连接，直到创建满30个连接
- 注意考虑一个进程可能会有多个SOFABOLT的通信实例，我们提供了全局开关以及用户开关两种方式：
[使用示例](./src/main/java/com/quark/sofa/connection/MultiAndPreConnection.java)

#### 2.4 自动断连和重连
通常RPC调用过程，是不需要断连和重连的。因为每次PRC调用过程，都会检验是否有可用连接，如果没有则新建一个。但有一些场景，是需要断链和保持长连接的:
 - 自动断连：比如通过LVS VIP或者F5建立多个连接的场景，因为网络设备的负载均衡机制，有可能某一些连接固定映射到某几台后端的RS上面，此时需要自动断连，
   然后重连，靠建连过程的随机性来实现最终的负载均衡。注意，开启了自动断连的场景，通常需要和重连配合使用。
 - 重连：比如客户端发起建连后，由服务端来通过双工通信，发起请求到客户端。此时如果没有重连机制，则无法实现。
 
 [使用示例](./src/main/java/com/quark/sofa/connection/MultiAndPreConnection.java)
 ````
 //通过系统属相来开和关，如果一个进程有多个RpcClient，则同时生效
 System.setProperty(Config.CONN_MONITOR_SWITCH,"true");
 System.setProperty(Config.CONN_RECONNECT_SWITCH,"true");
 //通过用户开关来开和关，只对当前RpcClient实例起作用
 client.enableReconnectSwitch();
 client.enableConnctionMonitorSwitch();
 ````
 
 #### 2.5序列化与反序列化器
 默认序列化和反序列化目前我们推荐使用的是Hessian，单考虑到不同的场景需求，我们支持默认序列化器的扩展，以及自定序列化器的功能特性。
 - 扩展序列化器：实现一个继承Serializer的序列化器，然后通过SerializerManager注册，指定一个index。
 ````
// 1. 实现 Serializer
public class HessianSerializer implements Serializer {
     @Override
     public byte[] serialize(Object obj) throws CodecException {
         ...
     }

     @Override
     public <T> T deserialize(byte[] data, String classOfT) throws CodecException {
     }
}
// 2. 注册
public static final byte    Hessian2    = 1;
SerializerManager.addSerializer(Hessian2, new HessianSerializer());

// 3. 通过系统属性来设置生效
 System.setProperty(Configs.SERIALIZER, String.valueOf(Hessian2));
 ````
 - 自定义序列化器：实现一个CustomerSerializer类，可以针对Header，content做自定义的序列化和反序列化。同时我们在接口上提供了InvokeContext,
 因此序列化和反序列化的逻辑，可以根据请求上下文来做动态调整
  * [使用示例](./src/main/java/com/quark/sofa/serializer/CustomerSerializerTest.java)
  
### 高级功能
#### 3.1开启IO线程处理机制
默认情况下，我们使用最佳实践的线程模型来处理请求，即尽可能少的占用IO线程，担忧一些场景，比如计算过程非常简单，希望减少线程切换，尽可能大的增加IO吞吐
量的场景。此时我们提供了一个开关，来让业务处理也在IO线程执行。
- [使用示例](./src/main/java/com/quark/sofa/processor/CustomerThreadProcessor.java)

#### 3.2 启用用户处理多线程池机制
请求处理过程，默认是一个线程池，那么当这个线程池出现问题后，则会造成整体的吞吐量降低。而有些业务场景，希望对他们的核心的请求处理过程，单独分配一个线程
池。避免不同请求相互影响。sofabolt中提供了一个线程池选择器：
- 实现一个线程池选择器 [示例](./src/main/java/com/quark/sofa/selector/DefaultExecutorSelector.java)
- 然后设置到用户请求处理器里面，调用过程即可根据选择器的逻辑来选择对应的线程池，[示例](./src/main/java/com/quark/sofa/selector/DefaultSelectorTest.java)

#### 3.3 请求处理超时FailFast机制
当服务端接收到请求后，如果线程池队列的排队等待时间已经超过了客户端发起调用时设置的超时时间，那么本次调用可以直接丢弃，因为请求对于客户端来说已经无用了
（note:该机制对于oneway调用方式无效，因为oneway不需要设置超时时间）。默认情况下，该功能处于开启状态，考虑到有些用户需要自己来做是否丢弃请求的判断，
同时打印一些日志来自己做记录，sofabolt提供了一个开关来控制这个功能的：
````
 @Override
 public boolean timeoutDiscard() {
     return false;// true表示开启自动丢弃，false表示关闭自动丢弃，用户在之后的处理processor里，可自行决策
 }
````
- 判断超时与打印日志
````
 public class SimpleClientUserProcessor extends SyncUserProcessor<RequestBody> {
    @Override
    public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
        if(bizCtx.isRequestTimeout()){
           log.info("arrive time: {}", bizCtx.getArriveTimestamp());
           ...
        }
    }
 }
````

#### 3.3 定制协议
对于通信类型比较简单的场景，我们直接复用RPC通信协议，使用对应的通信类型，就能解决大部分问题。而有一些场景，比如消息中间件、数据库中间件等，有自己的
私有通信协议，以及大量的请求命令类型，此时就需要从头来定制协议。此时将Sofabolt用作一个协议框架和具备基础通信功能的组件，比如基础通信模块、连接管理等
功能时可以服用的；而协议相关部分需要自己来开发和实现:
[具体参考RPC协议实现内容](https://github.com/alipay/sofa-bolt/tree/master/src/main/java/com/alipay/remoting/rpc)
