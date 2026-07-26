package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.czspig.productcritic.config.DeepSeekProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DeepSeekAiProviderTest {

    @Test
    void shouldBuildJsonModeRequestWithStableGenerationSettings() {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setApiKey("test-key");
        properties.setTemperature(0.9);
        properties.setMaxTokens(3000);
        DeepSeekAiProvider provider = new DeepSeekAiProvider(
                properties,
                new ProductReviewOutputParser(new ReviewReportRuleEngine()),
                new ObjectMapper()
        );

        Map<String, Object> body = provider.requestBody("prompt");

        assertThat(body.get("model")).isEqualTo("deepseek-v4-flash");
        assertThat(body.get("response_format")).isEqualTo(Map.of("type", "json_object"));
        assertThat(body.get("temperature")).isEqualTo(0.2);
        assertThat(body.get("max_tokens")).isEqualTo(5000);
        assertThat(body.get("stream")).isEqualTo(false);
    }
}
