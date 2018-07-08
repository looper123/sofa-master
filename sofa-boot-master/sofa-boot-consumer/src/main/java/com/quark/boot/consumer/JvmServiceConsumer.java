package com.quark.boot.consumer;

import com.alipay.sofa.rpc.boot.runtime.param.BoltBindingParam;
import com.alipay.sofa.runtime.api.annotation.SofaReference;
import com.alipay.sofa.runtime.api.annotation.SofaReferenceBinding;
import com.alipay.sofa.runtime.api.annotation.SofaServiceBinding;
import com.alipay.sofa.runtime.api.aware.ClientFactoryAware;
import com.alipay.sofa.runtime.api.client.ClientFactory;
import com.alipay.sofa.runtime.api.client.ReferenceClient;
import com.alipay.sofa.runtime.api.client.param.BindingParam;
import com.alipay.sofa.runtime.api.client.param.ReferenceParam;
import com.quark.boot.facade.SampleJvmAnnotationService;
import com.quark.boot.facade.SampleJvmApiService;
import com.quark.boot.facade.SampleJvmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.Resource;
import javax.xml.ws.BindingType;
import java.util.ArrayList;
import java.util.List;

/**
 * xml 、注解、api三种方式消费服务
 */
public class JvmServiceConsumer implements ClientFactoryAware {

//    获取通过api方式发布的服务
    private ClientFactory clientFactory;

//   获取通过xml方式发布的服务
    @Autowired
    private SampleJvmService sampleJvmService;

//    获取通过注解方式发布的服务
    @SofaReference(uniqueId = "annotationImpl",binding = @SofaReferenceBinding(bindingType = "bolt"))
    private SampleJvmAnnotationService sampleJvmAnnotationService;

    public void init() {
//        调用xml方式发布的服务
        sampleJvmService.message();
//        调用注解方式发布的服务
        sampleJvmAnnotationService.message();
//        调用api方式发布的服务
        ReferenceClient referenceClient = clientFactory.getClient(ReferenceClient.class);
//      设置接口类型
        ReferenceParam<SampleJvmApiService> referenceParam = new ReferenceParam<>();
        referenceParam.setInterfaceType(SampleJvmApiService.class);
//        和provider对应的uniqueId
        referenceParam.setUniqueId("serviceApiImpl");
//        从zookeeper中获取服务
        BindingParam bindingParam = new BoltBindingParam();
        referenceParam.setBindingParam(bindingParam);
        SampleJvmApiService sampleJvmServiceClientImpl = referenceClient.reference(referenceParam);
        sampleJvmServiceClientImpl.message();
    }

    @Override
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }



}
