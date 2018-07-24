### 使用 SOFATracer 远程汇报数据到 Zipkin
#### 环境准备
- JDK7或JDK8
- 需要采用apache maven3.2.5或者以上的版本来编译

#### 引入SOFATracer
添加sofatracer依赖：
````xml
<dependency>
    <groupId>com.alipay.sofa</groupId>
    <artifactId>tracer-sofa-boot-starter</artifactId>
</dependency>
````

#### 启动zipkin服务端
启动zipkin服务端用来接收sofatracer会报的链路数据，并做展示。[zipkin server搭建快速入门](https://zipkin.io/pages/quickstart.html)
里面提供了三种方式(docker、java、from source) 可以根据自己喜好选一种

#### 配置Zipkin依赖
考虑到Zipkin的数据上报能力不是sofatracer默认开启的能力，所以期望使用sofatracer做数据上报时，需要添加下面的Zipkin的依赖：
````xml
 <dependency>
    <groupId>io.zipkin.java</groupId>
    <artifactId>zipkin</artifactId>
    <version>1.19.2</version>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter</groupId>
    <artifactId>zipkin-reporter</artifactId>
    <version>0.6.12</version>
</dependency>
````

#### 启用sofatracer汇报数据到zipkin
1. 配置 `com.alipay.sofa.tracer.zipkin.enabled=true` 激活 SOFATracer 数据上报到 Zipkin。
2. 配置Zipkin server端地址 `com.alipay.sofa.tracer.zipkin.baseUrl=http://${ip}:${port}`。
配置好上述两个项目后，即激活了远程上报能力。本示例中已经搭建好的Zipkin server端地址是`http://zipkin-cloud-3.host.net:9411`。

#### 运行
把工程导入到IDE中运行生成的工程里面的`main`方法(一般上在 XXXApplication 这个类中)启动应用，也可以直接在该工程的根目录下运行
`mvn spring-boot:run`，将会在控制台中看到启动日志：
````
2018-07-24 11:21:51.000  INFO 5088 --- [main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2018-07-24 11:21:51.055  INFO 5088 --- [main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2018-07-24 11:21:51.059  INFO 5088 --- [main] c.q.s.t.z.SofaTracerZipkinApplication    : Started SofaTracerZipkinApplication in 3.031 seconds (JVM running for 3.616)
````
在浏览器中输入 [http://localhost:8080/zipkin](http://localhost:8080/zipkin)来访问rest服务：
````json
{
	"content": "Hello, SOFATracer Zipkin Remote Report!",
	"id": 1,
	"success": true
}
````

#### 查看Zipkin服务端展示
打开 Zipkin 服务端界面，假设我们部署的 Zipkin 服务端的地址是 `http://172.26.234.21:9411`，打开 URL 并在`Annotations Query`中搜索
 zipkin(由于我们本地访问的地址是 localhost:8080/zipkin)，可以看到展示的链路图。

