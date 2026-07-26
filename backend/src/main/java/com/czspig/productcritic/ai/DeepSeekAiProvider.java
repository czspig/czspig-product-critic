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

        DeepSeekResponse response = callDeepSeek(prompt);
        try {
            if (response.truncated()) {
                throw new ProductReviewOutputParser.OutputParseException("OUTPUT_TRUNCATED", "DeepSeek 输出被截断，finish_reason=length");
            }
            return outputParser.parseStrictAndValidate(response.content(), objectMapper);
        } catch (ProductReviewOutputParser.OutputParseException firstError) {
            try {
                DeepSeekResponse repaired = callDeepSeek(buildRepairPrompt(response.content(), firstError));
                if (repaired.truncated()) {
                    throw new ProductReviewOutputParser.OutputParseException("OUTPUT_TRUNCATED", "DeepSeek repair 输出被截断，finish_reason=length");
                }
                return outputParser.parseStrictAndValidate(repaired.content(), objectMapper);
            } catch (RuntimeException repairError) {
                throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 已返回内容，但报告结构解析失败，请重试。");
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

    private DeepSeekResponse callDeepSeek(String prompt) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());

        RestClient client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> requestBody = requestBody(prompt);

        try {
            String responseBody = client.post()
                    .uri(properties.chatCompletionsUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return extractResponse(responseBody);
        } catch (RestClientResponseException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, httpErrorMessage(ex.getStatusCode()));
        } catch (RestClientException ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 网络连接失败或请求超时，请稍后再试");
        }
    }

    Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你必须输出合法 JSON 对象，不要使用 Markdown 代码块，不要输出解释文字。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", Math.min(properties.getTemperature(), 0.2),
                "max_tokens", Math.max(properties.getMaxTokens(), 5000),
                "stream", false
        );
    }

    private DeepSeekResponse extractResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 返回内容为空");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").path(0);
            String finishReason = choice.path("finish_reason").asText("");
            JsonNode content = choice.path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 返回内容为空");
            }
            return new DeepSeekResponse(content.asText(), finishReason, responseBody.length());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "DeepSeek 响应解析失败");
        }
    }

    private String buildRepairPrompt(String invalidOutput, ProductReviewOutputParser.OutputParseException error) {
        return """
                你不是重新生成评审，而是修复下面 DeepSeek 已输出的 JSON。
                只输出一个合法 JSON 对象，不要 Markdown 代码围栏，不要解释、前言或总结。

                修复规则：
                - 必须保留原有语义，只修复 JSON 语法、字段名、类型、枚举和缺失的结构。
                - 不得虚构新的市场数据、用户事实、访谈、营收、竞品证据或评分依据。
                - 所有数组不得为 null；没有内容用 []。
                - 分数字段必须是 0-100 整数，不要字符串。
                - dimensionScores 必须且只能包含 6 个固定维度。
                - evidence、deductions、missingInformation、improvementActions 每项最多 3 条。
                - providedFacts、inferences、criticalHypotheses、unknowns 每项最多 5 条。
                - implementationDifficulty 和 riskLevel 的 scoreDirection 必须是 HIGHER_IS_WORSE，其余维度必须是 HIGHER_IS_BETTER。

                校验错误：
                <validation_error>
                %s: %s
                </validation_error>

                目标结构：
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

                待修复原始输出：
                <invalid_output>
                %s
                </invalid_output>
                """.formatted(
                error.getType(),
                error.getMessage(),
                limit(invalidOutput, 12000)
        );
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

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    record DeepSeekResponse(String content, String finishReason, int responseLength) {
        boolean truncated() {
            return "length".equalsIgnoreCase(finishReason);
        }
    }
}
