### 转发标准的HTTP协议
`sofa-mosn/examples/http-sample` 样例工程演示了如何配置 SOFAMesh 来转发标准 HTTP 协议，而 SOFAMesh 之间的协议是 HTTP/2。
#### 准备
需要一个编译好的SOFAMesh程序：
````
cd ${projectpath}/pkg/mosn
go build
````
将编译好的程序移动到当前目录，目录结构如下
````
mosn        //Mesh程序
server.go   //HTTP Server
server.json //HTTP Server的配置
client.json //HTTP Client的配置
````

### 运行说明
#### 启动一个HTTP SERVER
````
go run server.go
````
#### 启动代理HTTP SERVER的Mesher
````
./mosn start -c server.json
````
#### 启动代理HTTP CLIENT的Mesher
````
./mosn start -c client.json
````
#### 使用CURL进行验证
- 按照默认的配置设置，HTTP SERVER监听本地`8080`端口,HTTP CLINET代理监听本地`2046`端口
- Mesher代理配置转发请求为Header中包含`service:com.alipay.test.TestService:1.0`
````
//直接访问 HTTP Sever，观察现象
curl http://127.0.0.1:8080
//能收到 HTTP Server 返回的结果
curl --header "service:com.alipay.test.TestService:1.0" http://127.0.0.1:2046
//不能收到 HTTP Server 返回的结果(其实是返回了 404 Not Found）
curl http://127.0.0.1:2046
````
- 可以按照说明修改配置，进行不同的测试与验证



