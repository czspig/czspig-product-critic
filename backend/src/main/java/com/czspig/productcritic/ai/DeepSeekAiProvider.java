package com.czspig.productcritic.ai;

import java.util.List;
import java.util.Map;

import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.config.DeepSeekProperties;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DeepSeekAiProvider implements AiProvider {

    private final DeepSeekProperties properties;
    private final ProductReviewOutputParser outputParser;
    private final ObjectMapper objectMapper;

    public DeepSeekAiProvider(
            DeepSeekProperties properties,
            ProductReviewOutputParser outputParser,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.outputParser = outputParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewReportDto review(CreateReviewRequest request, String prompt) {
        if (!properties.hasApiKey()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek API Key 未配置");
        }

        String content = callDeepSeek(prompt);
        try {
            return outputParser.parseAndValidate(content, objectMapper);
        } catch (RuntimeException firstError) {
            String repairedContent = callDeepSeek(buildRepairPrompt(content));
            return outputParser.parseAndValidate(repairedContent, objectMapper);
        }
    }

    @Override
    public String providerName() {
        return "deepseek";
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    private String callDeepSeek(String prompt) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());

        RestClient client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你必须输出合法 JSON 对象，不要使用 Markdown 代码块。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "stream", false
        );

        try {
            String responseBody = client.post()
                    .uri(properties.chatCompletionsUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return extractContent(responseBody);
        } catch (RestClientException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 调用失败，请稍后再试");
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 返回内容为空");
            }
            return content.asText();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 响应解析失败");
        }
    }

    private String buildRepairPrompt(String invalidOutput) {
        return """
                请把下面内容修复为合法 JSON 对象，只输出 JSON，不要输出解释文字或 Markdown。
                必须包含全部字段，字段名不能改；缺失字段请根据已有内容补合理兜底，不要留空。

                必须符合这个结构：
                {
                  "oneLineVerdict": "string",
                  "reviewTargetType": "NEW_IDEA | MATURE_PRODUCT | CLIENT_REQUIREMENT | UNCLEAR",
                  "goDecision": "CONTINUE | PIVOT | PAUSE",
                  "goDecisionReason": "string",
                  "beatScore": 0,
                  "positioningScore": 0,
                  "painPointAnalysis": "string",
                  "fakeDemandRisks": ["string"],
                  "featureRedundancyCheck": ["string"],
                  "coldStartProblems": ["string"],
                  "mvpSuggestions": ["string"],
                  "minimumBuildVersion": {
                    "goal": "string",
                    "coreFeatures": ["string"],
                    "excludedFeatures": ["string"],
                    "successMetric": "string",
                    "validationPlan": ["string"]
                  },
                  "developerPrompt": "string"
                }

                字段规则：
                - beatScore 和 positioningScore 必须是 0-100 整数。
                - 数组字段至少 1 项，最好 2-4 项。
                - reviewTargetType 和 goDecision 只能使用枚举值。
                - developerPrompt 必须可直接复制给 Codex/Cursor。

                待修复内容：
                <invalid_output>
                %s
                </invalid_output>
                """.formatted(invalidOutput == null ? "" : invalidOutput);
    }
}
