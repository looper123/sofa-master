package com.quark.sofa.processor;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.quark.sofa.Entity.RequestBody;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户自定义同步事件处理器
 */
public class SynServerProcessor extends SyncUserProcessor<RequestBody> {

    private final static Logger logger = LoggerFactory.getLogger(SynServerProcessor.class);

    private long delayMs;

    //    whether delay or not
    private boolean delaySwitch;

    //    executor
    private ThreadPoolExecutor executor;

    //    default is true
    private boolean timeoutDiscard = true;

    private AtomicInteger invokeTimes = new AtomicInteger();
    private AtomicInteger oneWayTimes = new AtomicInteger();
    private AtomicInteger syncTimes = new AtomicInteger();
    private AtomicInteger futrueTimes = new AtomicInteger();
    private AtomicInteger callbackTimes = new AtomicInteger();

    private String remoteAddr;
    private CountDownLatch latch = new CountDownLatch(1);

    public SynServerProcessor() {
        this.delaySwitch = false;
        this.delayMs = 0;
        this.executor = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("request-process-pool"));
    }

    public SynServerProcessor(long delayMs) {
        this();
        if (delayMs < 0) {
            throw new IllegalArgumentException("delay time illegal!");
        }
        this.delayMs = delayMs;
        this.delaySwitch = true;
    }


    public SynServerProcessor(long delay, int core, int max, int keepaliveSeconds,
                              int workQueue) {
        this(delay);
        this.executor = new ThreadPoolExecutor(core, max, keepaliveSeconds, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(workQueue), new NamedThreadFactory(
                "Request-process-pool"));
    }

    @Override
    public Object handleRequest(BizContext bizCtx, RequestBody request) throws Exception {
        logger.warn("request received:" + request + ",timeout:" + bizCtx.getClientTimeout() + ",arriveTimestamp:" + bizCtx.getArriveTimestamp());
        if (bizCtx.isRequestTimeout()) {
            String errMsg = "stop Process in server biz thread ,already timeout!";
            throw new Exception(errMsg);
        }
        this.remoteAddr = bizCtx.getRemoteAddress();
//        test biz context get connection
        Assert.assertNotNull(bizCtx.getConnection());
        Assert.assertTrue(bizCtx.getConnection().isFine());

        Long waitTime = (Long) bizCtx.getInvokeContext().get((InvokeContext.BOLT_PROCESS_WAIT_TIME));
        Assert.assertNotNull(waitTime);
        if (logger.isInfoEnabled()) {
            logger.info("server user processor process wait time {}", waitTime);
        }
        latch.countDown();
        logger.warn("server user processor say ,remote address is [" + this.remoteAddr + "].");
        Assert.assertEquals(RequestBody.class, request.getClass());
        processTimes(request);
        if (!delaySwitch) {
            return RequestBody.DEFAULT_SERVER_RETURN_STR;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RequestBody.DEFAULT_SERVER_RETURN_STR;
    }


    private void processTimes(RequestBody req) {
        this.invokeTimes.incrementAndGet();
        if (req.getMsg().equals(RequestBody.DEFAULT_ONEWAY_STR)) {
            this.oneWayTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_SYNC_STR)) {
            this.syncTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_FUTURE_STR)) {
            this.futrueTimes.incrementAndGet();
        } else if (req.getMsg().equals(RequestBody.DEFAULT_CALLBACK_STR)) {
            this.callbackTimes.incrementAndGet();
        }
    }

    @Override
    public String interest() {
        return RequestBody.class.getName();
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public boolean timeoutDiscard() {
        return this.timeoutDiscard;
    }

    public int getInvokeTimes() {
        return this.invokeTimes.get();
    }

    public int getInvokeTimesEachCallType(RequestBody.InvokeType type) {
        return new int[]{
                this.oneWayTimes.get(), this.syncTimes.get(), this.callbackTimes.get()
        }[type.ordinal()];
    }

    public String getRemoteAddr() throws InterruptedException {
        latch.await(100, TimeUnit.MICROSECONDS);
        return this.remoteAddr;
    }

    public boolean isTimeoutDiscard() {
        return timeoutDiscard;
    }

    public void setTimeoutDiscard(boolean timeOutDiscard) {
        this.timeoutDiscard = timeOutDiscard;
    }
}
