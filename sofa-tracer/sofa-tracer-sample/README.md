### 基于spring mvc的落地日志
#### 环境准备
创建一个基于spring boot的web项目，添加一个简单的rest服务，以便于启动后检查

#### 引入sofaboot
把`pom.xml`中的
````xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>${spring.boot.version}</version>
    <relativePath/> 
</parent>
````
替换成
````xml
<parent>
    <groupId>com.alipay.sofa</groupId>
    <artifactId>sofaboot-dependencies</artifactId>
    <version>2.4.0</version>
</parent>
````
然后，添加一个SOFATracer依赖：
````xml
<dependency>
    <groupId>com.alipay.sofa</groupId>
    <artifactId>tracer-sofa-boot-starter</artifactId>
    <!-- SOFABoot 版本统一管控 -->
</dependency>
````
在`application.properties`文件中添加配置
````
# Application Name
spring.application.name=SOFATracerSpringMVC
# logging path
logging.path=./logs
````
最后添加一个简单的controller
````java
@RestController
public class SampleRestController {

    private static final String TEMPLATE = "Hello, %s!";

    private final AtomicLong    counter  = new AtomicLong();

    /**
     * http://localhost:8080/springmvc
     * @param name name
     * @return map
     */
    @RequestMapping("/springmvc")
    public Map<String, Object> springmvc(@RequestParam(value = "name", defaultValue = "SOFATracer SpringMVC DEMO") String name) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("success", true);
        resultMap.put("id", counter.incrementAndGet());
        resultMap.put("content", String.format(TEMPLATE, name));
        return resultMap;
    }
}
````

#### 运行查看效果
可以将工程导入到 IDE 中运行生成的工程里面中的 main 方法（一般上在 XXXApplication 这个类中）启动应用，也可以直接在该工程的根目录下运行 
mvn spring-boot:run，将会在控制台中看到启动打印的日志：
````
2018-07-23 14:30:06.310  INFO 9592 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2018-07-23 14:30:06.356  INFO 9592 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2018-07-23 14:30:06.360  INFO 9592 --- [           main] c.q.sofa.tracer.SofaTracerApplication    : Started SofaTracerApplication in 2.798 seconds (JVM running for 3.223)
````
可以通过在浏览器中访问[http://localhost:8080/springmvc ](http://localhost:8080/springmvc)来访问REST服务，结果类似如下：
````json
{
	"content": "Hello, SOFATracer SpringMVC DEMO!",
	"id": 1,
	"success": true
}
````

#### 查看日志
在上面的 application.properties 里面，我们配置的日志打印目录是 ./logs 即当前应用的根目录（我们可以根据自己的实践需要配置），在当前工程的根目录
下可以看到类似如下结构的日志文件：
````
./logs
├── spring.log
└── tracelog
    ├── spring-mvc-digest.log
    ├── spring-mvc-stat.log
    ├── static-info.log
    └── tracer-self.log
````
通过访问[http://localhost:8080/springmvc](http://localhost:8080/springmvc)SofaTracer会记录每一次访问的摘要日志，可以打开
`spring-mvc-digest.log`看到具体的输出内容，而对于每一个输出字段的含义可以[参考这里](https://github.com/alipay/sofa-tracer/wiki/QuickStart)
````
{"success":true,"id":1,"content":"Hello, SOFATracer SpringMVC DEMO!"}
````
