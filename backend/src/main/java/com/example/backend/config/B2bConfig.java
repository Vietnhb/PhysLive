package com.example.backend.config;

import com.example.backend.config.properties.VnpayProperties;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@org.springframework.context.annotation.PropertySource("classpath:b2b.properties")
@EnableScheduling
@EnableConfigurationProperties(VnpayProperties.class)
public class B2bConfig {
    @Bean
    public HttpClient vnpayHttpClient(VnpayProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    }
}
