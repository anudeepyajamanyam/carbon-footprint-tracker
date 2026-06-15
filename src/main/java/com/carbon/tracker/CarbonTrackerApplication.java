package com.carbon.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CarbonTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarbonTrackerApplication.class, args);
    }
}
