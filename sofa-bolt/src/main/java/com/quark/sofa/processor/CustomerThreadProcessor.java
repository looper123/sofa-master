package com.quark.sofa.processor;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.quark.sofa.Entity.RequestBody;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 开启IO线程处理机制(自定义线程模型)
 */
public class CustomerThreadProcessor extends SyncUserProcessor<RequestBody>{

    private static final Logger logger = LoggerFactory.getLogger(CustomerThreadProcessor.class);

    private long delayMs;

    private boolean delaySwitch;

    private AtomicInteger invokeTimes = new AtomicInteger();

    private AtomicInteger onewayTimes = new AtomicInteger();

    private AtomicInteger syncTimes = new AtomicInteger();

    private AtomicInteger futureTimes = new AtomicInteger();

    private AtomicInteger callBackTimes = new AtomicInteger();

    private ThreadPoolExecutor executor;

    private  String remoteAddr;

    private CountDownLatch latch = new CountDownLatch(1);

    public CustomerThreadProcessor() {
        this.delaySwitch = false;
        this.delayMs = 0;
        this.executor = new ThreadPoolExecutor(1,3,60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Rpc-common-executor"));
    }

    public CustomerThreadProcessor(long delay) {
        this();
        if (delay < 0) {
            throw new IllegalArgumentException("delay time illegal!");
        }
        this.delaySwitch = true;
        this.delayMs = delay;
    }

    @Override
    public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
        String threadName = Thread.currentThread().getName();
        Assert.assertTrue(threadName.contains("Rpc-netty-server-worker"));
        logger.warn("Request revceived:"+request);
        this.remoteAddr = bizCtx.getRemoteAddress();
        long waitTime = (Long)bizCtx.getInvokeContext().get(InvokeContext.BOLT_PROCESS_WAIT_TIME);
        logger.warn("server user processor process wait time ["+waitTime+"]");
        latch.countDown();
        logger.warn("server user processor say,remote address is ["+this.remoteAddr+"]");
        Assert.assertEquals(RequestBody.class,request.getClass());
        processTimes(request);
        if(!delaySwitch){
            return RequestBody.DEFAULT_SERVER_RETURN_STR;
        }
        Thread.sleep(delayMs);
        return RequestBody.DEFAULT_SERVER_RETURN_STR;
    }

    private void processTimes(RequestBody req) {
        this.invokeTimes.incrementAndGet();
        if (req.getMsg().equals(RequestBody.DEFAULT_ONEWAY_STR)) {
            this.onewayTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_SYNC_STR)) {
            this.syncTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_FUTURE_STR)) {
            this.futureTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_CALLBACK_STR)) {
            this.callBackTimes.incrementAndGet();
        }
    }

    @Override
    public String interest() {
        return RequestBody.class.getName();
    }

    @Override
    public boolean processInIOThread() {
        return true;
    }

    public int getInvokeTimes() {
        return this.invokeTimes.get();
    }

    public int getInvokeTimesEachCallType(RequestBody.InvokeType type) {
        return new int[] { this.onewayTimes.get(), this.syncTimes.get(), this.futureTimes.get(),
                this.callBackTimes.get() }[type.ordinal()];
    }

    public String getRemoteAddr() throws InterruptedException {
        latch.await(100, TimeUnit.MILLISECONDS);
        return this.remoteAddr;
    }
}
