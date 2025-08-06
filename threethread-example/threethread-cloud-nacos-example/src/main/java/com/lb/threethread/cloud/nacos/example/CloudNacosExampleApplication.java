package com.lb.threethread.cloud.nacos.example;

import com.lb.threethread.spring.base.enable.EnableOneThread;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableOneThread
@SpringBootApplication
public class CloudNacosExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudNacosExampleApplication.class, args);
    }
}
