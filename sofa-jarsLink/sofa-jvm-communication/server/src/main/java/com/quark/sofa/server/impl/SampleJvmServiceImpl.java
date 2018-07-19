package com.quark.sofa.server.impl;

import com.alipay.sofa.runtime.api.annotation.SofaService;
import com.quark.sofa.facade.service.SampleJvmService;
import org.springframework.stereotype.Component;

@SofaService
@Component
public class SampleJvmServiceImpl implements SampleJvmService {
    @Override
    public String service() {
        return "this is server";
    }
}
