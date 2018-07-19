package com.quark.sofa.client.controller;

import com.alipay.sofa.runtime.api.annotation.SofaReference;
import com.quark.sofa.facade.service.SampleJvmService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @SofaReference
    private SampleJvmService sampleJvmService;

    @ResponseBody
    @RequestMapping("/hello")
    public String hello() {
        return sampleJvmService.service();
    }
}
