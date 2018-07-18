package com.quark.sofa.extra;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeCallback;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcResponseFuture;
import com.alipay.remoting.util.TraceLogUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 设置客户端/服务端的请求上下文
 */
public class RequestContext {
    static Logger logger = LoggerFactory.getLogger(RequestContext.class);

    BoltServer server;

    RpcClient client;

    int port = PortScan.select();
    String ip = "127.0.0.1";
    String addr = "127.0.0.1" + port;
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
    public void stop(){
        try {
            server.stop();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("stop server failed!:"+e);
        }
    }

    @Test
    public void testOneWay()throws InterruptedException{
        RequestBody req = new RequestBody(2,"hello world oneway");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                InvokeContext invokeContext = new InvokeContext();
                client.oneway(addr,req,invokeContext);
                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_LOCAL_IP));
                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_REMOTE_IP));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_LOCAL_PORT));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_REMOTE_PORT));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));
                logger.warn("CLIENT_CONN_CREATETIME:"
                        + invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));
                Thread.sleep(100);
            } catch (RemotingException e) {
               String errMsg = "RemotingException caught in oneway!";
               logger.error(errMsg,e);
               Assert.fail(errMsg);
            }
        }
        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testSync() throws InterruptedException {
        RequestBody req = new RequestBody(1, "hello world sync");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                InvokeContext invokeContext = new InvokeContext();
                String res = (String) client.invokeSync(addr, req, invokeContext, 3000);
                logger.warn("Result received in sync: " + res);
                Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR, res);

                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_LOCAL_IP));
                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_REMOTE_IP));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_LOCAL_PORT));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_REMOTE_PORT));

                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));
                logger.warn("CLIENT_CONN_CREATETIME:"
                        + invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));

                TraceLogUtil.printConnectionTraceLog(logger, "0af4232214701387943901253",
                        invokeContext);
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in sync!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            } catch (InterruptedException e) {
                String errMsg = "InterruptedException caught in sync!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testFuture() throws InterruptedException {
        RequestBody req = new RequestBody(2, "hello world future");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                InvokeContext invokeContext = new InvokeContext();
                RpcResponseFuture future = client.invokeWithFuture(addr, req, invokeContext, 3000);
                String res = (String) future.get();
                Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR, res);

                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_LOCAL_IP));
                Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_REMOTE_IP));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_LOCAL_PORT));
                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_REMOTE_PORT));

                Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));
                logger.warn("CLIENT_CONN_CREATETIME:"
                        + invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));

                TraceLogUtil.printConnectionTraceLog(logger, "0af4232214701387943901253",
                        invokeContext);
            } catch (RemotingException e) {
                String errMsg = "RemotingException caught in future!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            } catch (InterruptedException e) {
                String errMsg = "InterruptedException caught in future!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testCallback() throws InterruptedException {
        RequestBody req = new RequestBody(1, "hello world callback");
        final List<String> rets = new ArrayList<String>(1);
        for (int i = 0; i < invokeTimes; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            InvokeContext invokeContext = new InvokeContext();
            try {
                client.invokeWithCallback(addr, req, invokeContext, new InvokeCallback() {
                    Executor executor = Executors.newCachedThreadPool();

                    @Override
                    public void onResponse(Object result) {
                        logger.warn("Result received in callback: " + result);
                        rets.add((String) result);
                        latch.countDown();
                    }

                    @Override
                    public void onException(Throwable e) {
                        logger.error("Process exception in callback.", e);
                        latch.countDown();
                    }

                    @Override
                    public Executor getExecutor() {
                        return executor;
                    }

                }, 1000);

            } catch (RemotingException e) {
                latch.countDown();
                String errMsg = "RemotingException caught in callback!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                String errMsg = "InterruptedException caught in callback!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
            if (rets.size() == 0) {
                Assert.fail("No result! Maybe exception caught!");
            }
            Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR, rets.get(0));
            rets.clear();

            Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_LOCAL_IP));
            Assert.assertEquals("127.0.0.1", invokeContext.get(InvokeContext.CLIENT_REMOTE_IP));
            Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_LOCAL_PORT));
            Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_REMOTE_PORT));

            Assert.assertNotNull(invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));
            logger.warn("CLIENT_CONN_CREATETIME:"
                    + invokeContext.get(InvokeContext.CLIENT_CONN_CREATETIME));

            TraceLogUtil
                    .printConnectionTraceLog(logger, "0af4232214701387943901253", invokeContext);
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

}
