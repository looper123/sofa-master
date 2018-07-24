### SOFATRACER
SOFATracer是一个用于分布式系统调用跟踪的组件，通过统一的traceId将调用链路中的各种网络调用情况以日志的方式记录下来，以达到透视化网络调用的目的。这
些体质可用于故障的快速发现，服务治理等。

### 一、背景
在当下的技术架构实施中，统一采用面向服务的分布式架构，通过服务来支撑起一个个应用，而部署在应用中的各种服务通常都是用复杂大规模分布式集群实现的，同时，
这些应用又构建在不同的软件模块上，这些软件模块，有可能是由不同的团队开发，可能使用不同的编程语言实现，同时，这些应用又构建在不同的软件模块上，这些软件
模块，有可能是由不同的团队开发，可能使用不同的编程语言实现、有可能部署了几千台服务器。因此，就需要一些可以帮助理解各个应用的线上调用行为，并可以分析远
程调用性能的组件。

为了能够分析应用的线程调用行为以及调用性能，蚂蚁金服基于[openTracing规范](http://opentracing.io/documentation/pages/spec.html)提供了分布式链路跟踪SOFATracer的解决方案。

### 二、功能简介
为了解决在实施大规模微服务架构时的链路跟踪问题，SOFATracer提供了以下的能力：
#### 2.1基于OpenTracing规范提供分布式链路跟踪解决方案
基于[openTracing规范](http://opentracing.io/documentation/pages/spec.html)并扩展器能力提供链路跟踪的解决方案。各个框架或者组件都可以
基于此实现，通过在各个组件中埋点的方式来提供链路跟踪的能力。

#### 2.2提供了异步落地磁盘的日志打印能力
基于Disruptor高性能无锁循环队列，提供异步打印日志到本地磁盘的能力。框架或者组件能够在接入时，在异步日志打印的前提下可以自定义体质文件的输出格式。
SOFATracer提供了两种类似的日志打印类型即摘要日志和统计日志，摘要日志：每一次调用均会落地磁盘日志；统计日志：每隔一定时间间隔进行统计输出的日志。

#### 2.3支持日志自清除和滚动能力
异步落地磁盘的SOFATracer日志支持自清除和滚动能力，支持按照天清除和按照小时或者天滚动的能力

#### 2.4基于SLF4J MDC的扩展能力
SLF4J提供了MDC (mapped Diagnostic context)功能，可以支持用户定义和修改日志的输出格式以及内容。SOFATracer集成了SLF4J MDC功能，方便用户在只
简单修改日志配置文件即可输出当前Tracer上下文tracerId和spanId。

#### 2.5界面展示能力
SOFATracer可以将链路跟踪数据远程上报到开源产品Sipkin做分布式链路跟踪展示。

#### 2.6统一配置能力
配置文件中提供丰富的配置能力以定制化应用的个性需求。

###  三、基于sofatracer的快速入门
- [SOFATracer 示例工程（基于 Spring MVC 示例落地日志](./sofa-tracer-sample/README.md)
- [SOFATracer 示例工程（基于 Spring MVC 示例远程上报 Zipkin](./sofa-tracer-zipkin/README.md)
- [SOFATracer 示例工程（基于日志编程接口 SLF4J 示例打印 traceId](./sofa-tracer-slf4j/README.md)



