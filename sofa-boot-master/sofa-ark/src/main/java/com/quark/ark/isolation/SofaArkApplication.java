package com.quark.ark.isolation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@ImportResource({ "classpath*:spring/bean.xml" })
@SpringBootApplication
public class SofaArkApplication {

    public static void main(String[] args) {
        //SOFABoot Isolation
        SpringApplication.run(SofaArkApplication.class, args);
    }
}
