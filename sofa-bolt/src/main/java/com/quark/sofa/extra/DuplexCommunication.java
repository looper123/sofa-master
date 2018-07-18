package com.quark.sofa.extra;

import com.alipay.remoting.Connection;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.util.RemotingUtil;
import com.quark.sofa.Entity.BoltServer;
import com.quark.sofa.Entity.PortScan;
import com.quark.sofa.Entity.RequestBody;
import com.quark.sofa.processor.ConnectEventProcessor;
import com.quark.sofa.processor.DisConnectEventProcessor;
import com.quark.sofa.processor.SimpleClientUserProcessor;
import com.quark.sofa.processor.SimpleServerUserProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * connection/Address 双工通信
 */
public class DuplexCommunication {

    static Logger logger = LoggerFactory
            .getLogger(DuplexCommunication.class);

    BoltServer server;
    RpcClient client;

    int port = PortScan.select();
    String ip = "127.0.0.1";
    String addr = "127.0.0.1:" + port;

    int invokeTimes = 5;

    SimpleServerUserProcessor serverUserProcessor = new SimpleServerUserProcessor();
    SimpleClientUserProcessor clientUserProcessor = new SimpleClientUserProcessor();
    ConnectEventProcessor clientConnectProcessor = new ConnectEventProcessor();
    ConnectEventProcessor serverConnectProcessor = new ConnectEventProcessor();
    DisConnectEventProcessor clientDisConnectProcessor = new DisConnectEventProcessor();
    DisConnectEventProcessor serverDisConnectProcessor = new DisConnectEventProcessor();

    @Before
    public void init() {
        server = new BoltServer(port, true);
        server.start();
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT, serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE, serverDisConnectProcessor);
        server.registerUserProcessor(serverUserProcessor);

        client = new RpcClient();
        client.addConnectionEventProcessor(ConnectionEventType.CONNECT, clientConnectProcessor);
        client.addConnectionEventProcessor(ConnectionEventType.CLOSE, clientDisConnectProcessor);
        client.registerUserProcessor(clientUserProcessor);
        client.init();
    }

    @After
    public void stop() {
        try {
            server.stop();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("Stop server failed!", e);
        }
    }

//    connection双工通信
    @Test
    public void duplexCommunicationByConnect() throws Exception {
        Connection clientConn = client.createStandaloneConnection(ip, port, 1000);
        for (int i = 0; i < invokeTimes; i++) {
            RequestBody req1 = new RequestBody(1, RequestBody.DEFAULT_CLIENT_STR);
            String servers = (String) client.invokeSync(clientConn, req1, 1000);
            Assert.assertEquals(servers, RequestBody.DEFAULT_SERVER_RETURN_STR);
            Assert.assertNotNull(serverConnectProcessor.getConnection());
            Connection serverConn = serverConnectProcessor.getConnection();
            RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
            String clientres = (String) server.getRpcServer().invokeSync(serverConn, req, 1000);
            Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR);
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

//  address双工通信
    @Test
    public void duplexCommunicationByAddress() throws Exception{
        Connection clientConn = client.createStandaloneConnection(ip,port,1000);
        String remote = RemotingUtil.parseRemoteAddress(clientConn.getChannel());
        String local = RemotingUtil.parseLocalAddress(clientConn.getChannel());
        logger.warn("client say local:"+local);
        logger.warn("client say remote:"+remote);
        for (int i = 0; i < invokeTimes; i++) {
            RequestBody req1 = new RequestBody(1,RequestBody.DEFAULT_CLIENT_STR);
            String servers = (String)client.invokeSync(clientConn,req1,1000);
            Assert.assertEquals(servers,RequestBody.DEFAULT_SERVER_RETURN_STR);
            Assert.assertNotNull(serverConnectProcessor.getConnection());
            String remoteAddr = serverUserProcessor.getRemoteAddr();
            RequestBody req = new RequestBody(1,RequestBody.DEFAULT_SERVER_RETURN_STR);
            String clientres = (String)server.getRpcServer().invokeSync(remoteAddr,req,1000);
            Assert.assertEquals(clientres,RequestBody.DEFAULT_CLIENT_RETURN_STR);
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }
}
