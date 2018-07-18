package com.quark.sofa.serializer;

import com.alipay.remoting.DefaultCustomSerializer;
import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.rpc.ResponseCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalStringCustomSerializer extends DefaultCustomSerializer {

    private AtomicBoolean serialFlag = new AtomicBoolean();
    private AtomicBoolean deserialFlag = new AtomicBoolean();

    private byte contentSerializer = -1;
    private byte contentDeserializer = -1;

    @Override
    public <T extends ResponseCommand> boolean serializeContent(T response) {
        serialFlag.set(true);
        RpcResponseCommand rpcResp = (RpcResponseCommand) response;
        String str = (String) rpcResp.getResponseObject();
        try {
            rpcResp.setContent(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        contentSerializer = response.getSerializer();
        return true;
    }

    @Override
    public <T extends ResponseCommand> boolean deserializeContent(T response, InvokeContext invokeContext) {
        deserialFlag.set(true);
        RpcResponseCommand rpcResp = (RpcResponseCommand) response;
        try {
            rpcResp.setResponseObject(new String(rpcResp.getContent(), "UTF-8") + "RANDOM");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        contentDeserializer = response.getSerializer();
        return true;

    }

    public boolean isSerialized() {
        return this.serialFlag.get();
    }

    public boolean isDeserialized() {
        return this.deserialFlag.get();
    }

    public byte getContentSerializer() {
        return contentSerializer;
    }

    public byte getContentDeserialier() {
        return contentDeserializer;
    }

    public void reset() {
        this.contentSerializer = -1;
        this.contentDeserializer = -1;
        this.deserialFlag.set(false);
        this.serialFlag.set(false);
    }
}

