package com.czspig.productcritic.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-v4-flash";
    private int timeoutSeconds = 60;
    private int maxTokens = 3000;
    private double temperature = 0.6;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Duration timeout() {
        return Duration.ofSeconds(Math.max(timeoutSeconds, 5));
    }

    public String chatCompletionsUrl() {
        String normalizedBaseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://api.deepseek.com"
                : baseUrl.trim().replaceAll("/+$", "");
        return normalizedBaseUrl + "/chat/completions";
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
