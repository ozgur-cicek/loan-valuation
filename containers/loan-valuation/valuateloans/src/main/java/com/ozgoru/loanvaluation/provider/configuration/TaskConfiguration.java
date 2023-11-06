package com.ozgoru.loanvaluation.provider.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableTask
public class TaskConfiguration {

    @Bean
    public CommandLineRunner commandLineRunner() {

        return args -> {
            //TODO: Not implemented yet
        };
    }
}
