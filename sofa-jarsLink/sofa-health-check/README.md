### 简介

SOFABoot 扩展了 Spring Boot 的健康检查，详情请移步SOFABoot 文档 。本样例工程意在演示在合并部署时，如何集成 SOFABoot 健康检查组件。合并部署时的健康检查和单个 SOFABoot 应用的健康检查存在如下区别：

+ 静态合并部署时，Ark 包正常启动的前提必须是所有 Biz 都健康检查通过。
+ 使用 Jarslink2.0 运行时动态部署 Biz 时，只有健康检查通过才会部署成功。
+ 合并部署时，访问 Spring Boot 默认的 /health 会新增名为 multiApplicationHealthChecker 的检查项，用于检查所有的 Biz 健康状态，只有所有的 Biz 健康检查成功才认为健康检查通过。
+ [具体操作流程](http://www.sofastack.tech/sofa-boot/docs/sofa-jarslink-jarslink-health-demo)
