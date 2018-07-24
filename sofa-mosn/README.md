### MOSN Project
MOsn是一款采用Golang开发的service mesh数据代理，功能和定位类似Envoy，旨在提供分布式、模块化、智能化的代理能力。MOSN支持Envoy和Istio的API，
可以和Istio集成。SofaMesh中，我们使用MOSN代替Envoy

#### 核心能力
+ Istio集成
    * 集成IStio0.8版本Pilot V2 API，可基于全动态资源配置运行
+ 核心转发
    * 自包含的网络服务器
    * 支持TCP代理
    * 支持TProxy模式
+ 多协议
    * 支持HTTP/1.1,Http/2
    * 支持sofaRpc
    * 支持Dubbo协议(开发中)
    * 支持HSF协议(开发中)
+ 核心路由
    * 支持virtual host路由
    * 支持header/url/prefix路由
    * 支持基于host metadata的subset路由
    * 支持重试
+ 后端管理&负载均衡
    * 支持连接池
    * 支持熔断
    * 支持后端主动健康检查
    * 支持random/rr等负载策略
    * 支持基于host metadata的subset负载策略
+ 可观察性
    * 观察网络数据
    * 观察协议数据
+ TLS  
    * 支持HTTP/1.1 on TLS
    * 支持HTTP/2 on TLS
    * 支持SOFARPC on TLS
+ 进程管理
    * 支持平滑reload
    * 支持平滑升级
+ 扩展能力
    * 支持自定义私有协议
    * 支持TCP IO层，协议层面加入自定义扩展
#### 架构设计
MOSN 由 NET/IO、Protocol、Stream、Proxy 四个层次组成，其中
- NET/IO 用于底层的字节流传输：Net/IO 层是用来支撑上层功能的核心层，由 Listener 和 Connection 组成。其中 Listener 对端口进行监听，接收新连
接。 Connection 用来管理 listener 上 accept 的 tcp 连接，包括从 tcp conn 上读写数据等。
    * 如下为Listener和Connection的关系图: 
    ![undefine](https://raw.githubusercontent.com/alipay/sofa-mosn/master/docs/design/resource/NetIO.png)
如图中所示，每个 Listener 上可以创建多个 Connection, 同时为了满足一些定制的功能, listener 和 connection 均以 filter chain 的形式提供可
扩展机制,结构如下所示：
    + Listner:
      + Event listener
        + ListenerEventListener 用于订阅 Listener 上的重要事件，包括接收连 以及关闭监听等，在对应事件发生时其中 的函数会被调用，需要注意的是，
        这里的事件的执行并不会影响主流程
      + Filter 
        + ListenerFilter 其中的 filter 用于定义一些钩子函数，在对应的位置触发相应的回调来与核心模型进行交互， 它通过调用的状态码来影响主流程的执行，
        当前支持两个状态码，分别是 Continue 继续执行 以及StopIteration 停止当前的执行逻辑
    + Connection:
      + Event listener
        + ConnectionEventListener 在 conn 上相应事件发生时触发
      + Filter
        + ReadFilter 用于读取 conn 上到达的字节流
        + WriteFilter 用于将字节流写入到 conn 上

+ Protocol 用于协议的 decode/encode:Protocol 层用于处理不同协议的编解码，并连接 IO 层和 Stream 层之间的逻辑，做二进制流的转化。在具体的处
理中，MOSN 在收到二进制的数据后， 根据配置的下游协议选择对应的解码器，将协议decode 成 headers, body, 以及 trailer，之后通过 stream 层的回调
接口将数据上传到 stream 层进行 stream 创建，并在 proxy 层完成往 upstream 的封装后，会再次通过 stream 层调用 protocol 的编码器将数据包编码
成二进制流。

+ Stream 用于封装请求和响应，在一个 conn 上做连接复用:Stream layer 通过创建 stream 来实现一个连接上的多路复用，并对 stream 的 request 和
 response 进行关联和处理来实现通信的高效。 下图为 stream 和 connection 对应的关系图
    ![framework](https://raw.githubusercontent.com/alipay/sofa-mosn/master/docs/design/resource/stream.png) 
如图中所示，一个 Connection 对应一个 StreamConnection 用来管理多个 stream，在具体的实现中 StreamConnection 将 conn 上获取的数据 分发到
不同的协议的解码器上做解码，并生成具体的stream，其中 StreamConnectionEventListener 用来监听其上的事件，当前仅支持 "OnGoAway"   

而构成 stream layer 的核心数据结构 stream 具有如下的结构， 同时为了满足 stream 在 encode/decode 处理过程中扩展的需要，filter chain 被引入

+ Stream
    * Encoder
        + StreamSender 用来编码 request/response stream 为二进制然后发送， 'endStream' 标识符用来标志后序是否还有数据，在设置为 true 
        的情况下， 无需等待直接发送
          + StreamSenderFilter 在 stream 发送时候定义的一些钩子函数用来处理特定逻辑
          + StreamSenderFilterCallbacks
    * Decoder
        + StreamReceiver 用于处理接收到的二进制流，在 IO 接收到二进制并解码成对应的 request/response 后会被调用
          + StreamReceiverFilter 在 封装 stream 的时候定义的一些钩子函数来处理特定逻辑，例如 故障注入以及心跳处理等
          + StreamReceiverFilterCallbacks
    * Event listener
        + StreamEventListener 用于 stream 相关事件发生时的调用，比如 stream 被reset的时候
- 另外，Stream 没有预先设定的方向，所以 StreamSender 可作为 client 来 encode request 也可以作为 server 来 encode response，而 
StreamReceiver 在client 的场景下可以用来 decode response，在server的场景下可以用来 decode request
+ Proxy 做 downstream 和 upstream 之间 stream 的转发:Proxy 是实现请求转发的核心模型，通过将 stream 在 downStream 和 upStream 之间做
传递，并通过 stream id 做关联，实现 转发。同时 Proxy 中实现了路由逻辑，以及连接池管理等逻辑，可对后端进行管理

#### MOSN工作流程
下图展示的是使用 Sidecar 方式部署运行 MSON 的示意图，Service 和 MOSN 分别部署在同机部署的 Pod 上， 您可以在配置文件中设置 MOSN 的上游和下游
协议，协议在 HTTP、HTTP2.0、以及SOFA RPC 中选择，未来还将支持 DUBBO, HSF 等
    ![framework](https://raw.githubusercontent.com/alipay/sofa-mosn/master/docs/design/resource/MosnWorkFlow.png)
    
#### MOSN模块划分
   ![framework](https://raw.githubusercontent.com/alipay/sofa-mosn/master/docs/design/resource/MosnModules.png)
其中：
+  Starter, Server, Listener, Config, XDS 为 MOSN 启动模块，用于完成 MOSN 的运行
+  最左侧的 Hardware, NET/IO, Protocol, Stream, Proxy 为 MOSN 架构 中介绍的 MOSN 核心模块， 用来完成 Service MESH 的核心功能
+  Router 为 MOSN 的核心路由模块，支持的功能包括：
   *  VirtualHost 形式的路由表
   *  基于 subset 的子集群路由匹配
   *  路由重试以及重定向功能
+  Upstream 为后端管理模块，支持的功能包括：
   *  Cluster 动态更新
   *  Host 动态更新
   *  对 Cluster 的 主动/被动 健康检查
   *  熔断机制
   *  CDS/EDS 对接能力
+  Metrics 模块可对协议层的数据做记录和追踪
+  LoadBalance 当前支持 RR, Random, Subset LB, 等负载均衡算法
+  Mixer, FlowControl, Lab, Admin 模块为待开发模块

#### 内部数据流
MOSN 内部数据流如下图所示  
    ![undifine](https://raw.githubusercontent.com/alipay/sofa-mosn/master/docs/design/resource/MosnDataFlow.png)
+ NET/IO 监测连接和数据包的到来
+ Protocol 对数据包进行检测，并使用对应协议做 decode 处理
+ Streaming 对 decode 的数据包做二次封装为stream
+ Protocol 对封装的 stream 做 proxy

### 快速入门

#### 准备运行环境
   - 如果你使用容器运行MOSN，请先[安装docker](https://docs.docker.com/install/) 
   - 如果你使用本地机器，请先使用类unix环境
   - 安装[go的编译环境](http://docs.studygolang.com/doc/install)
   - 安装dep:参考[官方安装文档](https://golang.github.io/dep/docs/installation.html)
   
#### 代码获取
MSON的代码托管在github上
````
go get github.com/alipay/sofa-mosn
````

如果你的go get环境下载存在问题，请手动创建项目工程
````
# 进入GOPATH下的src目录
cd $GOPATH/src
# 创建 github.com/alipay 目录
mkdir -p github.com/alipay
cd github.com/alipay

# clone mosn代码
git clone git@github.com:alipay/sofa-mosn.git
cd sofa-mosn
````
最终MOSN的源码路径为
`
$GOPATH/src/github.com/alipay/sofa-mosn
`

#### 把GOLANG项目导入IDE，推荐GoLand

#### 编译代码
在项目根目录下执行如下命令编译 MOSN 的二进制文件：
````
dep ensure	   // dep速度较慢，耐心等待
make build         //使用docker编译
// or
make build-local   // 使用本地的go编译环境
````
完成后可以在 `build/bundles/${version}/binary` 目录下找到编译好的二进制文件。

#### 运行测试
在项目根目录下执行如下命令运行单元测试：
`make unit-test`
单独运行MOSN作为proxy转发的示例：
- 参考 `sofa-mosn/test/` 下的示例

#### 从配置文件启动MOSN
`mosn start -c '$CONFIG_FILE'`

#### 如何快速启动一个mosn的转发程序
参考`example`目录下的示例工程
- [以sofa proxy为例](./docs/RunMosnSofaProxy.md)
- [以http proxy为例](./docs/RunMosnHttpProxy.md)
