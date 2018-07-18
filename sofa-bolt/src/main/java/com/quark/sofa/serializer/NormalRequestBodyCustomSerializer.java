package com.quark.sofa.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.rpc.RequestCommand;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.quark.sofa.Entity.RequestBody;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalRequestBodyCustomSerializer  extends DefaultCustomSerializer{

    private AtomicBoolean serialFlag = new AtomicBoolean();
    private AtomicBoolean deserialFlag = new AtomicBoolean();

    private byte contentSerializer = -1;
    private byte contentDeSerializer = -1;

    @Override
    public <T extends RequestCommand> boolean serializeContent(T req, InvokeContext invokeContext){
        deserialFlag.set(true);
        RpcRequestCommand rpcReq = (RpcRequestCommand)req;
        byte[]  content = rpcReq.getContent();
        ByteBuffer bb = ByteBuffer.wrap(content);
        int a = bb.getInt();
        byte[]  dst = new byte[content.length - 4];
        bb.get(dst,0,dst.length);
        try {
            String b = new String(dst,"UTF-8");
            RequestBody bd = new RequestBody(a, b);
            rpcReq.setRequestObject(bd);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        contentDeSerializer = rpcReq.getSerializer();
        return true;
    }

    public boolean isSerialized(){
        return this.serialFlag.get();
    }

    public boolean isDeSerialized(){
        return this.deserialFlag.get();
    }

    public byte getContentSerializer(){
        return contentSerializer;
    }

    public byte getContentDeSerializer(){
        return contentDeSerializer;
    }

    public void reset(){
        this.contentDeSerializer = -1;
        this.contentSerializer = -1;
        this.deserialFlag.set(false);
        this.serialFlag.set(false);
    }
}
