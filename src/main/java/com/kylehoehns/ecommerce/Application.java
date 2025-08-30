package com.kylehoehns.ecommerce;

import com.embabel.agent.config.annotation.EnableAgentMcpServer;
import com.embabel.agent.config.annotation.EnableAgentShell;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableAgentMcpServer
//@EnableAgentShell
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}