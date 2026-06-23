package com.czspig.productcritic.ai;

import java.util.Collection;

import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.dto.ReviewReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ProductReviewOutputParser {

    public ReviewReportDto parseAndValidate(String rawContent, ObjectMapper objectMapper) {
        try {
            ReviewReportDto report = objectMapper.readValue(extractJsonObject(rawContent), ReviewReportDto.class);
            validate(report);
            return report;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 输出结构解析失败");
        }
    }

    public void validate(ReviewReportDto report) {
        if (report == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审报告为空");
        }
        requireText(report.getOneLineVerdict(), "一句话评价不能为空");
        requireScore(report.getBeatScore(), "毒打指数");
        requireScore(report.getPositioningScore(), "产品定位评分");
        requireText(report.getPainPointAnalysis(), "用户痛点分析不能为空");
        requireList(report.getFakeDemandRisks(), "伪需求风险不能为空");
        requireList(report.getFeatureRedundancyCheck(), "功能冗余检查不能为空");
        requireList(report.getColdStartProblems(), "冷启动问题不能为空");
        requireList(report.getMvpSuggestions(), "MVP 改造建议不能为空");
        if (report.getMinimumBuildVersion() == null) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "最小可开发版本不能为空");
        }
        requireText(report.getMinimumBuildVersion().getGoal(), "最小可开发版本目标不能为空");
        requireList(report.getMinimumBuildVersion().getCoreFeatures(), "最小可开发版本核心功能不能为空");
        requireList(report.getMinimumBuildVersion().getExcludedFeatures(), "最小可开发版本排除功能不能为空");
        requireText(report.getDeveloperPrompt(), "开发 Prompt 不能为空");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, message);
        }
    }

    private void requireScore(Integer value, String fieldName) {
        if (value == null || value < 0 || value > 100) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, fieldName + "必须在 0-100 之间");
        }
    }

    private void requireList(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, message);
        }
    }

    private String extractJsonObject(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 输出为空");
        }
        String normalized = rawContent.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized
                    .replaceFirst("^```(?:json)?", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 输出不是 JSON 对象");
        }
        return normalized.substring(start, end + 1);
    }
}
