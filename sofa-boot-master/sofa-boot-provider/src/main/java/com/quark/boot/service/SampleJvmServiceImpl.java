package com.quark.boot.service;

import com.quark.boot.facade.SampleJvmService;

/**
 * xml方式发布服务
 */
public class SampleJvmServiceImpl implements SampleJvmService {

    public String message() {
        String message = "this service is published by xml";
        System.out.println("this service is published by xml");
        return message;
    }

}
