package com.ozgoru.loanvaluation.provider.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.dsl.task.Task;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableTask
public class TaskConfiguration {

    private static final String NAME_REQUEST_LOANS = "request-loans";
    private static final String NAME_VALUATE_LOANS = "valuate-loans";

    private static final String URI_REQUEST_LOANS = "docker:ozgoru/loanvaluation-requestloans:3.0.0";
    private static final String URI_VALUATE_LOANS = "docker:ozgoru/loanvaluation-valuateloans:3.0.0";

    @Autowired
    private DataFlowOperations dataFlowOperations;

    @Bean
    public CommandLineRunner commandLineRunner() {

        return args -> {
            dataFlowOperations.appRegistryOperations()
                    .register(
                            NAME_REQUEST_LOANS,
                            ApplicationType.task,
                            URI_REQUEST_LOANS,
                            null,
                            AppBootSchemaVersion.BOOT2,
                            true
                    );

            dataFlowOperations.appRegistryOperations()
                    .register(
                            NAME_VALUATE_LOANS,
                            ApplicationType.task,
                            URI_VALUATE_LOANS,
                            null,
                            AppBootSchemaVersion.BOOT2,
                            true
                    );

            var requestLoansTask = Task.builder(dataFlowOperations).findByName(NAME_REQUEST_LOANS);
            if (requestLoansTask.isPresent()) {
                requestLoansTask.get().destroy();
            }
            Task.builder(dataFlowOperations)
                    .name(NAME_REQUEST_LOANS)
                    .definition(NAME_REQUEST_LOANS)
                    .description("Request loans by calling external services.")
                    .build();

            var valuateLoansTask = Task.builder(dataFlowOperations).findByName(NAME_VALUATE_LOANS);
            if (valuateLoansTask.isPresent()) {
                valuateLoansTask.get().destroy();
            }
            Task.builder(dataFlowOperations)
                    .name(NAME_VALUATE_LOANS)
                    .definition(NAME_VALUATE_LOANS)
                    .description("Valuate loans by using external services.")
                    .build();
        };
    }
}
