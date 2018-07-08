package com.quark.boot;

import com.alipay.sofa.runtime.api.annotation.SofaReference;
import com.quark.boot.facade.SampleJvmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
public class ConsumerApplicationRun {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ConsumerApplicationRun.class);
        springApplication.run(args);
    }
}
