package com.quark.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProviderApplicationRun {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ProviderApplicationRun.class);
        springApplication.run(args);
    }
}
