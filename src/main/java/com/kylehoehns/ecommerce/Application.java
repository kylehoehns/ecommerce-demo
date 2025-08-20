package com.kylehoehns.ecommerce;

import com.embabel.agent.config.annotation.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
//@EnableAgentShell
@EnableAgentMcpServer
@EnableAgents(
    mcpServers = {McpServers.DOCKER_DESKTOP}
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}