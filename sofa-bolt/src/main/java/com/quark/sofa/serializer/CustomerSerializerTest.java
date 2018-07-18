package com.quark.sofa.serializer;

import com.alipay.remoting.*;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcResponseFuture;
import com.alipay.remoting.util.RemotingUtil;
import com.quark.sofa.Entity.BoltServer;
import com.quark.sofa.Entity.PortScan;
import com.quark.sofa.Entity.RequestBody;
import com.quark.sofa.connection.MultiAndPrevious;
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
 * 自定义序列化器
 */
public class CustomerSerializerTest {

    static Logger logger                    = LoggerFactory
            .getLogger(MultiAndPrevious.class);

    BoltServer server;
    RpcClient client;

    int                       port                      = PortScan.select();
    String                    ip                        = "127.0.0.1";
    String                    addr                      = "127.0.0.1:" + port;

    int                       invokeTimes               = 5;

    SimpleServerUserProcessor serverUserProcessor       = new SimpleServerUserProcessor();
    SimpleClientUserProcessor clientUserProcessor       = new SimpleClientUserProcessor();
    ConnectEventProcessor clientConnectProcessor    = new ConnectEventProcessor();
    ConnectEventProcessor     serverConnectProcessor    = new ConnectEventProcessor();
    DisConnectEventProcessor clientDisConnectProcessor = new DisConnectEventProcessor();
    DisConnectEventProcessor  serverDisConnectProcessor = new DisConnectEventProcessor();

    @Before
    public void init(){
        server = new BoltServer(port);
        server.start();
        server.addConnectionEventProcessor(ConnectionEventType.CONNECT,serverConnectProcessor);
        server.addConnectionEventProcessor(ConnectionEventType.CLOSE,serverDisConnectProcessor);
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
            logger.error("Stop server failed!", e);
        }
    }

    @Test
    public void testOneway()throws InterruptedException{
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(),s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(),s2);
        RequestBody req = new RequestBody(2,"hello world oneway");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                byte testCodec = (byte)i;
                InvokeContext invokeContext = new InvokeContext();
                invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER,testCodec);
                client.oneway(addr,req,invokeContext);
                Assert.assertEquals(testCodec,s1.getContentSerializer());
                Assert.assertEquals(-1,s2.getContentDeserialier());
                Thread.sleep(100);
            } catch (RemotingException e) {
               String errMsg = "remotingException caught in oneway!";
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
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        RequestBody req = new RequestBody(1, "hello world sync");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                byte testCodec = (byte) i;
                InvokeContext invokeContext = new InvokeContext();
                invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, testCodec);
                String res = (String) client.invokeSync(addr, req, invokeContext, 3000);
                logger.warn("Result received in sync: " + res);
                Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM", res);

                Assert.assertEquals(testCodec, s1.getContentSerializer());
                Assert.assertEquals(testCodec, s2.getContentSerializer());
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
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        RequestBody req = new RequestBody(2, "hello world future");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                byte testCodec = (byte) i;
                InvokeContext invokeContext = new InvokeContext();
                invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, testCodec);

                RpcResponseFuture future = client.invokeWithFuture(addr, req, invokeContext, 3000);
                String res = (String) future.get();
                Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM", res);

                Assert.assertEquals(testCodec, s1.getContentSerializer());
                Assert.assertEquals(testCodec, s2.getContentSerializer());
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
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        RequestBody req = new RequestBody(1, "hello world callback");
        final List<String> rets = new ArrayList<String>(1);
        for (int i = 0; i < invokeTimes; i++) {
            final CountDownLatch latch = new CountDownLatch(1);

            byte testCodec = (byte) i;
            InvokeContext invokeContext = new InvokeContext();
            invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, testCodec);

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
            Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM", rets.get(0));
            rets.clear();

            Assert.assertEquals(testCodec, s1.getContentSerializer());
            Assert.assertEquals(testCodec, s2.getContentSerializer());
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testServerSyncUsingConnection() throws Exception {
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        Connection clientConn = client.createStandaloneConnection(ip, port, 1000);

        for (int i = 0; i < invokeTimes; i++) {
            byte testCodec = (byte) i;
            InvokeContext invokeContext = new InvokeContext();
            invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, testCodec);

            RequestBody req1 = new RequestBody(1, RequestBody.DEFAULT_CLIENT_STR);
            String serverres = (String) client.invokeSync(clientConn, req1, invokeContext, 1000);
            Assert.assertEquals(serverres, RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM");

            Assert.assertEquals(testCodec, s1.getContentSerializer());
            Assert.assertEquals(testCodec, s2.getContentSerializer());

            Assert.assertNotNull(serverConnectProcessor.getConnection());
            Connection serverConn = serverConnectProcessor.getConnection();

            invokeContext.clear();
            invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, (byte) (testCodec + 1));
            s1.reset();
            s2.reset();

            RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
            String clientres = (String) server.getRpcServer().invokeSync(serverConn, req,
                    invokeContext, 1000);
            Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR + "RANDOM");

            Assert.assertEquals(testCodec + 1, s1.getContentSerializer());
            Assert.assertEquals(testCodec + 1, s2.getContentSerializer());
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testServerSyncUsingAddress() throws Exception {
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        Connection clientConn = client.createStandaloneConnection(ip, port, 1000);
        String remote = RemotingUtil.parseRemoteAddress(clientConn.getChannel());
        String local = RemotingUtil.parseLocalAddress(clientConn.getChannel());
        logger.warn("Client say local:" + local);
        logger.warn("Client say remote:" + remote);

        for (int i = 0; i < invokeTimes; i++) {
            byte testCodec = (byte) i;
            InvokeContext invokeContext = new InvokeContext();
            invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, testCodec);

            RequestBody req1 = new RequestBody(1, RequestBody.DEFAULT_CLIENT_STR);
            String serverres = (String) client.invokeSync(clientConn, req1, invokeContext, 1000);
            Assert.assertEquals(serverres, RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM");

            Assert.assertEquals(testCodec, s1.getContentSerializer());
            Assert.assertEquals(testCodec, s2.getContentSerializer());

            invokeContext.clear();
            invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, (byte) (testCodec + 1));
            s1.reset();
            s2.reset();

            Assert.assertNotNull(serverConnectProcessor.getConnection());
            // only when client invoked, the remote address can be get by UserProcessor
            // otherwise, please use ConnectionEventProcessor
            String remoteAddr = serverUserProcessor.getRemoteAddr();
            RequestBody req = new RequestBody(1, RequestBody.DEFAULT_SERVER_STR);
            String clientres = (String) server.getRpcServer().invokeSync(remoteAddr, req,
                    invokeContext, 1000);
            Assert.assertEquals(clientres, RequestBody.DEFAULT_CLIENT_RETURN_STR + "RANDOM");

            Assert.assertEquals(testCodec + 1, s1.getContentSerializer());
            Assert.assertEquals(testCodec + 1, s2.getContentSerializer());
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

    @Test
    public void testIllegalType() throws Exception {
        NormalRequestBodyCustomSerializer s1 = new NormalRequestBodyCustomSerializer();
        NormalStringCustomSerializer s2 = new NormalStringCustomSerializer();
        CustomSerializerManager.registerCustomSerializer(RequestBody.class.getName(), s1);
        CustomSerializerManager.registerCustomSerializer(String.class.getName(), s2);

        RequestBody req = new RequestBody(1, "hello world sync");
        for (int i = 0; i < invokeTimes; i++) {
            try {
                byte testCodec = (byte) i;
                InvokeContext invokeContext = new InvokeContext();
                invokeContext.put(InvokeContext.BOLT_CUSTOM_SERIALIZER, (int) testCodec);
                String res = (String) client.invokeSync(addr, req, invokeContext, 3000);
                logger.warn("Result received in sync: " + res);
                Assert.assertEquals(RequestBody.DEFAULT_SERVER_RETURN_STR + "RANDOM", res);

                Assert.assertEquals(testCodec, s1.getContentSerializer());
                Assert.assertEquals(testCodec, s2.getContentSerializer());
            } catch (IllegalArgumentException e) {
                logger.error("IllegalArgumentException", e);
                Assert.assertTrue(true);
                return;
            } catch (InterruptedException e) {
                String errMsg = "InterruptedException caught in sync!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
            Assert.fail("Should not reach here!");
        }

        Assert.assertTrue(serverConnectProcessor.isConnected());
        Assert.assertEquals(1, serverConnectProcessor.getConnectTimes());
        Assert.assertEquals(invokeTimes, serverUserProcessor.getInvokeTimes());
    }

}
