package com.quark.boot.consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations = "classpath:service-consumer.xml")
public class ConsumerConfiguration {
}
