#### 介绍
SOFA-ARK是一款基于java实现的轻量级类隔离容器,主要为应用程序提供类隔离和依赖包隔离的能力；基于 Fat Jar 技术，应用可以被打包成一个自包含可运行的 Fat Jar，应用既可以是简单的单模块 Java 应用也可以是 Spring Boot 应用.
#### 快速入门 
https://alipay.github.io/sofastack.github.io/ 
#### 背景
日常使用 Java 开发，常常会遇到包依赖冲突的问题，尤其当工程应用变得臃肿庞大，包冲突的问题也会变得更加棘手，导致各种各样的报错，例如LinkageError, NoSuchMethodError等；实际开发中，可以采用多种方法来解决包冲突问题，比较常见的是类似 SpringBoot 的做法，统一管理应用所有依赖包的版本，保证这些三方包不存在依赖冲突；这种做法只能有效避免包冲突的问题，不能根本上解决包冲突的问题；如果某个应用的确需要在运行时使用两个相互冲突的包，例如 protobuf2 和 protobuf3，那么类似 SpringBoot 的做法依然解决不了问题；

为了彻底解决包冲突的问题，我们需要借助类隔离机制，使用不同的 Classloader 加载不同版本的三方依赖，进而隔离包冲突问题；OSGI 作为业内最出名的类隔离框架，自然是可以被用于解决上述包冲突问题，但是 OSGI 框架太过臃肿，功能繁杂；为了解决包冲突问题，引入 OSGI 框架，有牛刀杀鸡之嫌，反而使工程变得更加复杂，不利于开发；

SOFAArk 专注于解决类隔离问题，采用轻量级的类隔离方案来解决日常经常遇到的包冲突问题，在蚂蚁金服内部服务于整个 [SOFABoot](https://github.com/looper123/sofa-master) 技术体系，弥补 SpringBoot 没有的类隔离能力。实际上，SOFAArk 是一个通用的轻量级类隔离框架，并不限于 SpringBoot 应用，也可以和其他的 Java 开发框架集成；

#### 原理
![framework](https://raw.githubusercontent.com/alipay/sofa-ark/master/resource/SOFA-Ark-Framework.png)
 
在介绍这三个概念之前，为了统一术语，有必要先说一下所谓的 Ark 包；Ark 包是满足特定目录格式要求的 Executed Fat Jar，使用官方提供的 Maven 插件 sofa-ark-maven-plugin可以将工程应用打包成一个标准格式的 Ark 包；使用命令 java -jar application.jar即可在 Ark 容器之上启动应用；Ark 包 通常包含 Ark Container、Ark Plugin、 Ark Biz；以下我们针对这三个概念简单做下名词解释：
 
-  Ark Container: Ark 容器，负责整个运行时的管理；Ark Plugin 和 Ark Biz 运行在 Ark 容器之上；容器具备管理多插件、多应用的功能；容器启动成功后，会自动解析 classpath 包含的 Ark Plugin 和 Ark Biz 依赖，完成隔离加载并按优先级依次启动之；
 
-  Ark Plugin: Ark 插件，满足特定目录格式要求的 Fat Jar，使用官方提供的 Maven 插件 sofa-ark-plugin-maven-plugin 可以将一个或多个普通的 Java  Jar 包打包成一个标准格式的 Ark Plugin； Ark Plugin 会包含一份配置文件，通常包括插件类导入导出配置、插件启动优先级等；运行时，Ark 容器会使用独立的 PluginClassLoader 加载插件，并根据插件配置构建类加载索引表，从而使插件与插件、插件与应用之间相互隔离；
 
-  Ark Biz: Ark 业务模块，满足特定目录格式要求的 Fat Jar ，使用官方提供的 Maven 插件 sofa-ark-maven-plugin 可以将工程应用打包成一个标准格式的 Ark-Biz 包；是工程应用模块及其依赖包的组织单元，包含应用启动所需的所有依赖和配置；

#### ark plugin的四个概念
- 导入类：插件启动时，优先委托给导出该类的插件负责加载，如果加载不到，才会尝试从本插件内部加载；
- 导出类：其他插件如果导入了该类，优先从本插件加载；
- 导入资源：插件在查找资源时，优先委托给导出改资源的插件负责加载，如果加载不到，才会尝试从本插件内部加载；
- 导入资源：其他插件如果导入了该资源，优先从本插件加载；

#### 运行顺序 
 在运行时，Ark Container 优先启动，自动解析 classpath 包含的 Ark Plugin 和 Ark Biz，并读取他们的配置，构建类加载索引关系；然后使用独立的 Classloader 加载他们并按优先级配置依次启动；需要指出的是，Ark Plugin 优先 Ark Biz 被加载启动；Ark Plugin 之间是双向类索引关系，即可以相互委托对方加载所需的类；Ark Plugin 和 Ark Biz 是单向类索引关系，即只允许 Ark Biz 索引 Ark Plugin 加载的类，反之则不允许。
 
#### [executable-ark.jar目录结构说明](https://alipay.github.io/sofastack.github.io/docs/ark-jar.html)


#### 使用场景
SOFAArk初衷是为了解决包冲突问题，那什么情况下可以使用 SOFAArk 以及如何使用呢？ 假设如下场景，如果工程需要引入两个三方包：A 和 B，但是 A 需要依赖版本号为 0.1 的 C 包，而恰好 B 需要依赖版本号为 0.2 的 C 包，且 C 包的这两个版本无法兼容:
![framework](https://raw.githubusercontent.com/alipay/sofa-ark/master/resource/SOFA-Ark-Conflict.png)

此时，即可使用 SOFAArk 解决该依赖冲突问题；只需要把 A 和版本为 0.1 的 C 包一起打包成一个 Ark Plugin，然后让应用工程引入该插件依赖即可；

#### maven插件
````xml
<plugin>
    <groupId>com.alipay.sofa</groupId>
    <artifactId>sofa-ark-maven-plugin</artifactId>
</plugin>
````
- 详细配置见[pom](./pom.xml)
- note:需要指出的是：该插件配置只负责打包、发布 ark 包，如果需要在 ide 中启动 SOFAArk 容器或者在 SOFAArk 容器之上运行测试用例，需要额外添加类隔离框架依赖；

#### maven依赖
````xml
<dependency>
    <groupId>com.alipay.sofa</groupId>
    <artifactId>sofa-ark-springboot-starter</artifactId>
</dependency>
````
- 在实际开发中，为了在 SOFAArk 容器之上运行测试用例或者本地IDE中使用 SOFABoot 类隔离能力，需要引入该依赖：

##### 遇到的坑
- 本地的maven仓库地址不能带空格 否则会在启动获取jar包路径的时候产生乱码 并报错:file not exist！！

#### 操作流程
- 通过pom中的 <denyImportClasses> 、 <denyImportPackages> 、<denyImportResources>三个标签指定需要使用ark分离的包(存在冲突)，执行mvn 
clean install deploy命令，把包发布到仓库中，生成类似下面这种maven依赖：
````xml
<dependency>
     <groupId>com.alipay.sofa</groupId>
     <artifactId>sample-ark-plugin</artifactId>
     <classifier>ark-plugin</classifier>
     <version>0.4.0-SNAPSHOT</version>
 </dependency>
````
- 其中groupId、artifactId、version和该项目一一对应，classifier和sofa-ark-maven-plugin中的arkClassifier对应，最后把该依赖添加到项目的
pom文件中。
