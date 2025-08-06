package com.lb.threethread.nacos.cloud.example;

import com.lb.threethread.spring.base.enable.EnableOneThread;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableOneThread
@SpringBootApplication
public class NacosCloudExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosCloudExampleApplication.class, args);
    }
}
