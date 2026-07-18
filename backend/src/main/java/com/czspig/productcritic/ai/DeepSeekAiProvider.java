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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
            try {
                String repairedContent = callDeepSeek(buildRepairPrompt(content));
                return outputParser.parseAndValidate(repairedContent, objectMapper);
            } catch (RuntimeException repairError) {
                throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 输出不是可用的结构化 JSON，自动修复失败");
            }
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
                        Map.of("role", "system", "content", "你必须输出合法 JSON 对象，不要使用 Markdown 代码块，不要输出解释文字。"),
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
        } catch (RestClientResponseException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, httpErrorMessage(ex.getStatusCode()));
        } catch (RestClientException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 网络连接失败或请求超时，请稍后再试");
        }
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 返回内容为空");
        }
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
                必须保留旧字段，并补齐可信评审字段；缺失字段请根据已有内容做保守兜底，不要编造外部市场数据、用户访谈或营收事实。
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
                  "developerPrompt": "string",
                  "overallAssessment": {
                    "overallScore": 0,
                    "decision": "PROCEED | VALIDATE_FIRST | NARROW_SCOPE | PAUSE",
                    "strongestDimension": "string",
                    "weakestDimension": "string",
                    "mostUncertainDimension": "string",
                    "priorityQuestion": "string",
                    "overallReasoning": "string"
                  },
                  "dimensionScores": [
                    {
                      "dimensionKey": "painPoint | marketSpace | businessModel | implementationDifficulty | growthPotential | riskLevel",
                      "dimensionName": "string",
                      "score": 0,
                      "confidence": "HIGH | MEDIUM | LOW",
                      "scoreDirection": "HIGHER_IS_BETTER | HIGHER_IS_WORSE",
                      "summary": "string",
                      "evidence": ["string"],
                      "deductions": ["string"],
                      "missingInformation": ["string"],
                      "improvementActions": ["string"],
                      "changeReason": "string"
                    }
                  ],
                  "knowledgeBoundary": {
                    "providedFacts": [{"statement": "string", "sourceText": "string"}],
                    "inferences": [{"statement": "string", "basis": "string", "confidence": "HIGH | MEDIUM | LOW"}],
                    "criticalHypotheses": [{"statement": "string", "importance": "HIGH | MEDIUM | LOW", "impactIfFalse": "string", "relatedDimensions": ["string"]}],
                    "unknowns": [{"question": "string", "reason": "string"}]
                  },
                  "qualityAudit": {
                    "overallStatus": "PASS | WARNING | FAIL",
                    "summary": "string",
                    "checks": [{"key": "string", "name": "string", "status": "PASS | WARNING | FAIL", "message": "string", "relatedPaths": ["string"]}],
                    "issues": [{"severity": "HIGH | MEDIUM | LOW", "type": "string", "message": "string", "suggestion": "string"}]
                  }
                }

                字段规则：
                - beatScore、positioningScore、overallScore、dimensionScores.score 必须是 0-100 整数。
                - dimensionScores 必须包含 6 个固定维度：painPoint、marketSpace、businessModel、implementationDifficulty、growthPotential、riskLevel。
                - implementationDifficulty 和 riskLevel 的 scoreDirection 必须是 HIGHER_IS_WORSE，其余维度必须是 HIGHER_IS_BETTER。
                - providedFacts 只能写用户输入中能定位到原文的信息；无法确认的内容放入 inferences、criticalHypotheses 或 unknowns。
                - qualityAudit 可以先给初值，后端会再次规则审计。

                待修复内容：
                <invalid_output>
                %s
                </invalid_output>
                """.formatted(invalidOutput == null ? "" : invalidOutput);
    }

    private String httpErrorMessage(HttpStatusCode statusCode) {
        int status = statusCode == null ? 0 : statusCode.value();
        if (status == 401 || status == 403) {
            return "DeepSeek 鉴权失败，请检查 API Key 是否有效";
        }
        if (status == 429) {
            return "DeepSeek 调用触发限流，请稍后再试或检查额度";
        }
        if (status >= 500) {
            return "DeepSeek 服务暂时不可用，请稍后再试";
        }
        return "DeepSeek 调用失败，HTTP 状态码 " + status;
    }
}
