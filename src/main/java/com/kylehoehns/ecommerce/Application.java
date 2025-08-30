package com.kylehoehns.ecommerce;

import com.embabel.agent.config.annotation.EnableAgentMcpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableAgentMcpServer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}