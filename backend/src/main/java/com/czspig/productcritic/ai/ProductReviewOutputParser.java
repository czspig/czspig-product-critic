package com.czspig.productcritic.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            normalize(report);
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
        requireReviewTargetType(report.getReviewTargetType());
        requireGoDecision(report.getGoDecision());
        requireText(report.getGoDecisionReason(), "是否继续做的理由不能为空");
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
        requireText(report.getMinimumBuildVersion().getSuccessMetric(), "最小可开发版本成功指标不能为空");
        requireList(report.getMinimumBuildVersion().getValidationPlan(), "最小可开发版本验证计划不能为空");
        requireText(report.getDeveloperPrompt(), "开发 Prompt 不能为空");
    }

    private void normalize(ReviewReportDto report) {
        if (report == null) {
            return;
        }
        report.setReviewTargetType(validReviewTargetType(report.getReviewTargetType()) ? report.getReviewTargetType() : "UNCLEAR");
        report.setGoDecision(validGoDecision(report.getGoDecision()) ? report.getGoDecision() : "PAUSE");
        report.setBeatScore(clamp(report.getBeatScore(), 75));
        report.setPositioningScore(clamp(report.getPositioningScore(), 35));
        report.setOneLineVerdict(defaultText(
                report.getOneLineVerdict(),
                "当前输入还需要收缩目标用户、使用场景和最小验证闭环。"
        ));
        report.setGoDecisionReason(defaultText(
                report.getGoDecisionReason(),
                "模型输出缺少决策理由，系统已兜底为暂停推进：请先补充目标用户、场景、风险和成功指标。"
        ));
        report.setPainPointAnalysis(defaultText(
                report.getPainPointAnalysis(),
                "当前报告缺少痛点分析。请先明确目标用户、痛点频率、替代方案和用户为什么现在必须解决。"
        ));
        report.setFakeDemandRisks(defaultList(
                report.getFakeDemandRisks(),
                "可能存在用户嘴上说需要、实际不愿持续使用的伪需求风险。",
                "请用 5-10 个目标用户访谈或手工交付实验验证核心假设。"
        ));
        report.setFeatureRedundancyCheck(defaultList(
                report.getFeatureRedundancyCheck(),
                "第一版应砍掉登录支付、复杂后台、社区、分享裂变等非核心功能。",
                "只保留能验证一次核心任务闭环的最小功能。"
        ));
        report.setColdStartProblems(defaultList(
                report.getColdStartProblems(),
                "第一批用户来源、触达方式和使用动机仍需明确。",
                "如果依赖内容、数据或供需双方，请先用人工方式验证冷启动路径。"
        ));
        report.setMvpSuggestions(defaultList(
                report.getMvpSuggestions(),
                "把第一版收缩到一个目标用户、一个高频场景和一次可完成的核心任务。",
                "用可观察指标判断是否继续开发，而不是先堆功能。"
        ));

        if (report.getMinimumBuildVersion() == null) {
            report.setMinimumBuildVersion(new ReviewReportDto.MinimumBuildVersion());
        }
        ReviewReportDto.MinimumBuildVersion minimum = report.getMinimumBuildVersion();
        minimum.setGoal(defaultText(minimum.getGoal(), "验证目标用户是否愿意完成一次核心任务并留下可复用结果。"));
        minimum.setCoreFeatures(defaultList(
                minimum.getCoreFeatures(),
                "产品想法输入",
                "结构化评审报告",
                "历史记录查看"
        ));
        minimum.setExcludedFeatures(defaultList(
                minimum.getExcludedFeatures(),
                "登录支付",
                "分享裂变",
                "复杂后台"
        ));
        minimum.setSuccessMetric(defaultText(
                minimum.getSuccessMetric(),
                "至少 5 位目标用户完成核心流程，并愿意基于结果继续调整想法。"
        ));
        minimum.setValidationPlan(defaultList(
                minimum.getValidationPlan(),
                "找 5-10 位目标用户访谈。",
                "用手工或最小页面交付一次核心结果。",
                "记录是否愿意再次使用或付出迁移成本。"
        ));
        report.setDeveloperPrompt(defaultText(
                report.getDeveloperPrompt(),
                "请基于现有仓库实现最小可运行版本，只保留核心输入、评审结果和历史查看，不做登录、支付、分享裂变或复杂后台。"
        ));
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

    private void requireGoDecision(String value) {
        if (!validGoDecision(value)) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "是否继续做必须是 CONTINUE、PIVOT 或 PAUSE");
        }
    }

    private void requireReviewTargetType(String value) {
        if (!validReviewTargetType(value)) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "评审对象类型必须是 NEW_IDEA、MATURE_PRODUCT、CLIENT_REQUIREMENT 或 UNCLEAR");
        }
    }

    private void requireList(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, message);
        }
    }

    private boolean validGoDecision(String value) {
        return "CONTINUE".equals(value) || "PIVOT".equals(value) || "PAUSE".equals(value);
    }

    private boolean validReviewTargetType(String value) {
        return "NEW_IDEA".equals(value)
                || "MATURE_PRODUCT".equals(value)
                || "CLIENT_REQUIREMENT".equals(value)
                || "UNCLEAR".equals(value);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> defaultList(List<String> values, String... fallback) {
        if (values == null) {
            return new ArrayList<>(List.of(fallback));
        }
        List<String> cleaned = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return cleaned.isEmpty() ? new ArrayList<>(List.of(fallback)) : new ArrayList<>(cleaned);
    }

    private Integer clamp(Integer value, int fallback) {
        int safe = value == null ? fallback : value;
        return Math.max(0, Math.min(100, safe));
    }

    private String extractJsonObject(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 输出为空");
        }
        String normalized = rawContent.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized
                    .replaceFirst("^```(?:json|JSON)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
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
