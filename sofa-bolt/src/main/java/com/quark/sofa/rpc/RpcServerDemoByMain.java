package com.quark.sofa.rpc;

import com.alipay.remoting.ConnectionEventType;
import com.quark.sofa.Entity.BoltServer;
import com.quark.sofa.processor.ConnectEventProcessor;
import com.quark.sofa.processor.DisConnectEventProcessor;
import com.quark.sofa.processor.SimpleServerUserProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端初始化模型
 */
public class RpcServerDemoByMain {

    static Logger logger = LoggerFactory.getLogger(RpcServerDemoByMain.class);

    BoltServer server;

    int port = 8999;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor();
    ConnectEventProcessor serverConnectProcessor = new ConnectEventProcessor();
    DisConnectEventProcessor serverDisConnectProcessor = new DisConnectEventProcessor();

    public RpcServerDemoByMain() {
        //创建rpc服务端实例
        server = new BoltServer(port);
        //添加processor
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        //注册用户processor
        server.registerUserProcessor(serverUserProcessor);
        // 4. server start
        if (server.start()) {
            System.out.println("server start ok!");
        } else {
            System.out.println("server start failed!");
        }
        // server.getRpcServer().stop();
    }

    public static void main(String[] args) {
        new RpcServerDemoByMain();
    }
}
