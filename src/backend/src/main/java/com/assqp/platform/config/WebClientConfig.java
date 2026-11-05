package com.assqp.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.api.url:}")
    private String aiApiUrl;

    @Value("${sonar.host.url:}")
    private String sonarHostUrl;

    @Value("${zap.api.url:}")
    private String zapApiUrl;

    @Value("${ml.service.url:}")
    private String mlServiceUrl;

    @Bean
    public WebClient aiWebClient() {
        WebClient.Builder builder = WebClient.builder();
        if (aiApiUrl != null && !aiApiUrl.isBlank()) {
            builder.baseUrl(aiApiUrl);
        }
        return builder.build();
    }

    @Bean
    public WebClient sonarWebClient() {
        WebClient.Builder builder = WebClient.builder();
        if (sonarHostUrl != null && !sonarHostUrl.isBlank()) {
            builder.baseUrl(sonarHostUrl);
        }
        return builder.build();
    }

    @Bean
    public WebClient zapWebClient() {
        WebClient.Builder builder = WebClient.builder();
        if (zapApiUrl != null && !zapApiUrl.isBlank()) {
            builder.baseUrl(zapApiUrl);
        }
        return builder.build();
    }

    @Bean
    public WebClient mlWebClient() {
        WebClient.Builder builder = WebClient.builder();
        if (mlServiceUrl != null && !mlServiceUrl.isBlank()) {
            builder.baseUrl(mlServiceUrl);
        }
        return builder.build();
    }
}
