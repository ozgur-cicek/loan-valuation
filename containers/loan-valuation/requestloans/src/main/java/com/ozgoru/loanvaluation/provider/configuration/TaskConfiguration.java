package com.ozgoru.loanvaluation.provider.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableTask
public class TaskConfiguration {

    // TODO: We might define this on the task definition
    private static final String LOAN_PROVIDER_URL = "http://loan-provider:8090";

    private static final Logger LOGGER = Logger.getLogger(TaskConfiguration.class.getName());

    @Autowired
    private Environment environment;

    @Bean
    public CommandLineRunner commandLineRunner() {

        final var cutoffDate = environment.getProperty("cutoff_date");

        if (cutoffDate == null) {
            LOGGER.log(Level.INFO, "cutoff_date not passed to the task.");

            throw new IllegalStateException("Missing application property: cutoff_date.");
        }

        return args -> {
            // Post to export
            HttpRequest exportRequest = HttpRequest.newBuilder()
                    .uri(new URI(String.format(LOAN_PROVIDER_URL + "/loans/export?cutoff_date=%s", cutoffDate)))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> exportResponse = HttpClient
                    .newBuilder()
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(exportRequest, HttpResponse.BodyHandlers.ofString());

            // Get the status until successful result if possible
            if (exportResponse.statusCode() == 202) {
                var response = new ObjectMapper().readValue(exportResponse.body(), ExportResponse.class);

                HttpRequest statusRequest = HttpRequest.newBuilder()
                        .uri(new URI(LOAN_PROVIDER_URL + String.format("/loans/status?export_id=%s", response.getExportId())))
                        .GET()
                        .build();

                while (true) {
                    Thread.sleep(10000);

                    HttpResponse<String> statusResponse = HttpClient
                            .newBuilder()
                            .proxy(ProxySelector.getDefault())
                            .build()
                            .send(statusRequest, HttpResponse.BodyHandlers.ofString());

                    if (statusResponse.statusCode() == 200) {
                        LOGGER.log(Level.INFO, "Loans are requested and exported successfully");

                        break;
                    } else {
                        LOGGER.log(Level.INFO, "Loans are requested but not exported yet");
                    }
                }
            } else {
                LOGGER.log(Level.INFO, String.format("Status code: %s", exportResponse.statusCode()));

                throw new IllegalStateException(String.format("Export request response is not as expected: %s", exportResponse.body()));
            }
        };
    }

    public static class ExportResponse {

        @JsonProperty("export_id")
        private UUID exportId;

        public ExportResponse() {
        }

        public UUID getExportId() {
            return exportId;
        }

        public void setExportId(UUID exportId) {
            this.exportId = exportId;
        }
    }
}
