package com.quark.boot.service;

import com.quark.boot.facade.SampleJvmApiService;

public class SampleJvmApiServiceImpl implements SampleJvmApiService {
    @Override
    public String message() {
        String message = "this service published by api";
        System.out.println(message);
        return message;
    }
}
