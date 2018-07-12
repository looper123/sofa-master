package com.quark.sofa.rpc;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.quark.sofa.Entity.RequestBody;
import com.quark.sofa.processor.ConnectEventProcessor;
import com.quark.sofa.processor.DisConnectEventProcessor;
import com.quark.sofa.processor.SimpleClientUserProcessor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端初始化模型
 */
public class RpcClientDemoByMain {
    static Logger logger  = LoggerFactory.getLogger(RpcClientDemoByMain.class);

    static RpcClient client;

    static String addr = "127.0.0.1:8999";

    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor();
    ConnectEventProcessor clientConnectProcessor    = new ConnectEventProcessor();
    DisConnectEventProcessor clientDisConnectProcessor = new DisConnectEventProcessor();

    public RpcClientDemoByMain(){
        //构造
        client = new RpcClient();
        //给客户端添加processor
        client.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
        client.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        //客户端初始化
        client.init();
    }

    public static void main(String[] args) {
        new RpcClientDemoByMain();
        RequestBody req = new RequestBody(2, "hello world sync");
        try {
            String res = (String) client.invokeSync(addr, req, 3000);
            System.out.println("invoke sync result = [" + res + "]");
        } catch (RemotingException e) {
            String errMsg = "RemotingException caught in oneway!";
            logger.error(errMsg, e);
            Assert.fail(errMsg);
        } catch (InterruptedException e) {
            logger.error("interrupted!");
        }
        client.shutdown();
    }
}
