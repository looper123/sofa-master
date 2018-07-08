package com.quark.boot.service;

import com.alipay.sofa.rpc.boot.runtime.param.BoltBindingParam;
import com.alipay.sofa.runtime.api.aware.ClientFactoryAware;
import com.alipay.sofa.runtime.api.client.ClientFactory;
import com.alipay.sofa.runtime.api.client.ServiceClient;
import com.alipay.sofa.runtime.api.client.param.BindingParam;
import com.alipay.sofa.runtime.api.client.param.ServiceParam;
import com.quark.boot.facade.SampleJvmApiService;
import com.quark.boot.facade.SampleJvmService;

import java.util.ArrayList;
import java.util.List;

/**
 * api方式发布服务
 */
public class PublishServiceWithClient implements ClientFactoryAware {

    private ClientFactory clientFactory;

    public void init() {
        ServiceClient serviceClient = clientFactory.getClient(ServiceClient.class);
        ServiceParam serviceParam = new ServiceParam();
        serviceParam.setInstance(new SampleJvmApiServiceImpl());
        serviceParam.setInterfaceType(SampleJvmApiService.class);
        serviceParam.setUniqueId("serviceApiImpl");
//        更改注册中心
        List<BindingParam> bindParams = new ArrayList<>();
        BindingParam bindingParam = new BoltBindingParam();
        bindParams.add(bindingParam);
        serviceParam.setBindingParams(bindParams);
        serviceClient.service(serviceParam);
    }

    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }
}
