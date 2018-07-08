package com.quark.boot.service;

import com.alipay.sofa.runtime.api.annotation.SofaService;
import com.alipay.sofa.runtime.api.annotation.SofaServiceBinding;
import com.quark.boot.facade.SampleJvmAnnotationService;
import com.quark.boot.facade.SampleJvmService;

/**
 * 注解方式发布服务
 */
@SofaService(uniqueId = "annotationImpl" ,bindings = @SofaServiceBinding(bindingType ="bolt" ))
public class SampleJvmServiceAnnotationImpl implements SampleJvmAnnotationService {

    public String message() {
        String message = "this service published by annotation";
        System.out.println(message);
        return message;
    }
}
