package com.lumichat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LumiChatApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumiChatApiApplication.class, args);
    }
}
