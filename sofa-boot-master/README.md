### sofa-boot开发目的
#### 1 增强spring-boot的健康检查能力
    针对 Spring Boot 缺少 Readiness Check 能力的情况，SOFABoot 增加了 Spring Boot 现有的健康检查的能力，提供了 Readiness Check 的能力。利用 Readiness Check 的能力，SOFA 中间件中的各个组件只有在 Readiness Check 通过之后，才将流量引入到应用的实例中，比如 RPC，只有在 Readiness Check 通过之后，才会向服务注册中心注册，后面来自上游应用的流量才会进入。除了中间件可以利用 Readiness Check 的事件来控制流量的进入之外，PAAS 系统也可以通过访问 http://localhost:8080/health/readiness 来获取应用的 Readiness Check 的状况，用来控制例如负载均衡设备等等流量的进入。
#### 2 提供类隔离能力   
    为了解决 Spring Boot 下的类依赖冲突的问题，SOFABoot 基于 SOFAArk 提供了 Spring Boot 上的类隔离的能力，在一个 SOFABoot 的系统中，只要引入 SOFAArk 相关的依赖，就可以将 SOFA 中间件相关的类和应用相关的类的 ClassLoader 进行隔离，防止出现类冲突。当然，用户也可以基于 SOFAArk，将其他的中间件、第三方的依赖和应用的类进行隔离。
#### 3 日志空间隔离能力
    为了统一大规模微服务场景下的中间件日志的打印，SOFABoot 提供了日志空间隔离的能力给 SOFA 中间件，SOFA 中间件中的各个组件采用日志空间隔离的能力之后，自动就会将本身的日志和应用的普通日志隔离开来，并且打印的日志的路径也是相对固定，非常方便进行统一地监控。    
#### 4 sofa中间件的集成管理
    基于 Spring Boot 的自动配置能力，SOFABoot 提供了 SOFA 中间件统一易用的编程接口以及 Spring Boot 的 Starter，方便在 Spring Boot 环境下使用 SOFA 中间件，SOFA 中间件中的各个组件都是独立可插拔的，节约开发时间，和后期维护的成本。
#### 5 模块化开发
    SOFABoot 从 2.4.0 版本开始支持基于 Spring 上下文隔离的模块化开发能力，每个 SOFABoot 模块使用独立的 Spring 上下文，避免不同 SOFABoot 模块间的 BeanId 冲突，有效降低企业级多模块开发时团队间的沟通成本。
      
### 环境准备
- jdk 1.7/1.8
- maven 3.2.5及以上

### 创建工程
- 推荐使用spring boot生成工具生成项目

### 引入依赖
- 删除原来项目中的spring-boot父依赖
```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>${spring.boot.version}</version>
        <relativePath/> 
    </parent>
```
加上sofa对应的依赖<br>
基础依赖
```xml
    <parent>
        <groupId>com.alipay.sofa</groupId>
        <artifactId>sofaboot-dependencies</artifactId>
        <version>2.4.1</version>
    </parent>  
```    
健康检查扩展模块 
````xml
    <dependency>
        <groupId>com.alipay.sofa</groupId>
        <artifactId>healthcheck-sofa-boot-starter</artifactId>
    </dependency>
````

#### 配置文件
- 在application.properties文件中添加sofa-boot必要参数
````
    # Application Name
    spring.application.name=SOFABoot Demo
    # logging path
    logging.path=./logs
````
#### 运行main方法 查看效果
````
2018-07-04 18:18:36.164  INFO 10684 --- [main] o.s.j.e.a.AnnotationMBeanExporter: Registering beans for JMX exposure on startup
2018-07-04 18:18:36.173  INFO 10684 --- [main] o.s.c.support.DefaultLifecycleProcessor: Starting beans in phase 0
2018-07-04 18:18:36.289  INFO 10684 --- [main] s.b.c.e.t.TomcatEmbeddedServletContainer: Tomcat started on port(s): 8080 (http)
2018-07-04 18:18:36.293  INFO 10684 --- [main] com.quark.boot.SofaBootApplication: Started SofaBootApplication in 2.78 seconds (JVM running for 3.746)
````
#### 通过浏览器获取当前sofa工程使用的maven版本信息&readliness check信息
输入:http://localhost:8080/sofaboot/versions<br>
````json
   [{
   	"GroupId": "com.alipay.sofa",
   	"Doc-Url": "https://github.com/alipay/sofa-boot",
   	"ArtifactId": "infra-sofa-boot-starter",
   	"Bulit-Time": "2018-06-29T21:25:45+0800",
   	"Commit-Time": "2018-06-29T20:08:06+0800",
   	"Commit-Id": "1f79d79565d0eab9dae499a19331718b92fdcb5f",
   	"Version": "2.4.1"
   }]
````
````json
{
	"status": "UP",
	"sofaBootComponentHealthCheckInfo": {
		"status": "UP"
	},
	"springContextHealthCheckInfo": {
		"status": "UP"
	},
	"DiskSpaceHealthIndicator": {
		"status": "UP",
		"total": 892824944640,
		"free": 844583006208,
		"threshold": 10485760
	}
}
````
#### log目录结构
- 因为在application.properties中配置的日志保存在项目根目录下
````
./logs
├── health-check
│   ├── sofaboot-common-default.log
│   └── sofaboot-common-error.log
├── infra
│   ├── common-default.log
│   └── common-error.log
└── spring.log
````    
#### sofa提供了单元测试工具
````xml
    <dependency>
        <groupId>com.alipay.sofa</groupId>
        <artifactId>test-sofa-boot-starter</artifactId>
    </dependency>
````

#### SOFA-RPC
- SOFARPC 是一个高可扩展性、高性能、生产级的 Java RPC 框架。在蚂蚁金服 SOFARPC 已经经历了十多年及五代版本的发展。SOFARPC 致力于简化应用之间的 RPC 调用，为应用提供方便透明、稳定高效的点对点远程服务调用方案。为了用户和开发者方便的进行功能扩展，SOFARPC 提供了丰富的模型抽象和可扩展接口，包括过滤器、路由、负载均衡等等。同时围绕 SOFARPC 框架及其周边组件提供丰富的微服务治理方案。

#### 功能特性
-  透明化、高性能的远程服务调用
-  支持多种服务路由及负载均衡策略
-  支持多种注册中心的集成
-  支持多种协议，包括 Bolt、Rest、Dubbo 等
-  支持同步、单向、回调、泛化等多种调用方式
-  支持集群容错、服务预热、自动故障隔离
-  强大的扩展功能，可以按需扩展各个功能组件
