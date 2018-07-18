package com.quark.sofa.connection;

import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.InvokeCallback;
import com.alipay.remoting.Url;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcAddressParser;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.RpcResponseFuture;
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
 * 建立多连接和链接预热
 */
public class MultiAndPrevious {

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
    DisConnectEventProcessor  clientDisConnectProcessor = new DisConnectEventProcessor();
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

    private void doTest(String url, RequestBody.InvokeType type,int invokeTimes,int expectConnTimes,int expectMaxFirstInvokeTimeDuration){
        try {
            RpcAddressParser parser = new RpcAddressParser();
            final Url addr = parser.parse(url);
            for (int i = 0; i < invokeTimes; i++) {
                long start = System.currentTimeMillis();
                String ret = (String) doInvoke(type, url);
                long end = System.currentTimeMillis();
                logger.warn("WITH WARMUP, first invoke cost ->" + (end - start));
                if ((end - start) > expectMaxFirstInvokeTimeDuration
                        && expectMaxFirstInvokeTimeDuration != -1) {
                    Assert.fail("Should not reach here, First invoke cost too much time ["
                            + (end - start) + "ms], expect limit in ["
                            + expectMaxFirstInvokeTimeDuration + "ms]!");
                }
                if (!type.equals(RequestBody.InvokeType.ONEWAY)) {
                    Assert.assertEquals(ret, RequestBody.DEFAULT_SERVER_RETURN_STR);
                }
            }

            if (addr.isConnWarmup()) {
                Thread.sleep(200);// must wait, to wait event finish
                Assert.assertEquals(expectConnTimes, serverConnectProcessor.getConnectTimes());
                Assert.assertEquals(expectConnTimes, clientConnectProcessor.getConnectTimes());

                client.closeConnection(addr);
                Thread.sleep(200);// must wait, to wait event finish
                Assert
                        .assertEquals(expectConnTimes, serverDisConnectProcessor.getDisConnectTimes());
                Assert
                        .assertEquals(expectConnTimes, clientDisConnectProcessor.getDisConnectTimes());
            } else {
                Thread.sleep(200);// must wait, to wait event finish
                Assert.assertTrue(serverConnectProcessor.getConnectTimes() >= expectConnTimes);
                Assert.assertTrue(clientConnectProcessor.getConnectTimes() >= expectConnTimes);

                client.closeConnection(addr);
                Thread.sleep(200);// must wait, to wait event finish
                Assert
                        .assertTrue(serverDisConnectProcessor.getDisConnectTimes() >= expectConnTimes);
                Assert
                        .assertTrue(clientDisConnectProcessor.getDisConnectTimes() >= expectConnTimes);
            }
        } catch (RemotingException e) {
            logger.error("Exception caught in sync!", e);
            Assert.fail("Should not reach here!");
        } catch (InterruptedException e) {
            logger.error("InterruptedException in sync", e);
            Assert.fail("Should not reach here!");
        }
    }

    /**
     * do invoke
     *
     * @param type
     * @param url
     * @return
     * @throws RemotingException
     * @throws InterruptedException
     */
    private Object doInvoke(RequestBody.InvokeType type, String url) throws RemotingException,
            InterruptedException {
        RequestBody b1 = new RequestBody(1, "hello world");
        Object obj = null;
        if (type.equals(RequestBody.InvokeType.ONEWAY)) {
            client.oneway(url, b1);
        } else if (type.equals(RequestBody.InvokeType.SYNC)) {
            obj = client.invokeSync(url, b1, 3000);
        } else if (type.equals(RequestBody.InvokeType.FUTURE)) {
            RpcResponseFuture future = client.invokeWithFuture(url, b1, 3000);
            obj = future.get(3000);
        } else if (type.equals(RequestBody.InvokeType.CALLBACK)) {
            final List<String> rets = new ArrayList<String>(1);
            final CountDownLatch latch = new CountDownLatch(1);
            client.invokeWithCallback(url, b1, new InvokeCallback() {
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

            }, 3000);
            try {
                latch.await();
            } catch (InterruptedException e) {
                String errMsg = "InterruptedException caught in callback!";
                logger.error(errMsg, e);
                Assert.fail(errMsg);
            }
            if (rets.size() == 0) {
                Assert.fail("No result of callback! Maybe exception caught!");
            }
            obj = rets.get(0);
        }
        return obj;

    }

    /**
     * reset all times
     */
    private void doResetTimes() {
        this.clientConnectProcessor.reset();
        this.serverConnectProcessor.reset();
        this.clientDisConnectProcessor.reset();
        this.serverDisConnectProcessor.reset();
    }

    @Test
    public void test_connNum_10_warmup_True_invoke_1times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=10&_CONNECTIONWARMUP=true";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 10, -1);
        }
    }

    @Test
    public void test_connNum_10_warmup_False_invoke_1times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=10&_CONNECTIONWARMUP=false";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 1, -1);
        }
    }

    @Test
    public void test_connNum_1_warmup_True_invoke_1times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=1&_CONNECTIONWARMUP=true";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 1, -1);
        }
    }

    @Test
    public void test_connNum_1_warmup_False_invoke_3times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=1&_CONNECTIONWARMUP=false";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 1, -1);
        }
    }

    @Test
    public void test_connNum_2_warmup_False_invoke_3times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=2&_CONNECTIONWARMUP=false";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 1, -1);
        }
    }

    @Test
    public void test_connNum_2_warmup_True_invoke_3times() {
        String url = addr
                + "?_CONNECTTIMEOUT=1000&_TIMEOUT=5000&_CONNECTIONNUM=2&_CONNECTIONWARMUP=true";
        for (RequestBody.InvokeType type : RequestBody.InvokeType.values()) {
            doResetTimes();
            doTest(url, type, 1, 2, -1);
        }
    }
}

