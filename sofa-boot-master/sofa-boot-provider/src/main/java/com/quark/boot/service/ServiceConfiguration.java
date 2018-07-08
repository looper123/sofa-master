package com.quark.boot.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations = "classpath:service-provider.xml")
public class ServiceConfiguration {
}
