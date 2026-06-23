package com.czspig.productcritic.ai;

import java.util.List;

import com.czspig.productcritic.common.ReviewMode;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public ReviewReportDto review(CreateReviewRequest request, String prompt) {
        ReviewMode mode = ReviewMode.requireValid(request.getMode());
        int roastLevel = request.getRoastLevel();
        String ideaName = summarizeIdea(request.getContent());

        ReviewReportDto report = new ReviewReportDto();
        report.setOneLineVerdict(oneLineVerdict(mode, roastLevel));
        report.setBeatScore(clamp(60 + roastLevel * 6 + request.getContent().length() % 11));
        report.setPositioningScore(clamp(64 + request.getContent().length() % 17 - roastLevel));
        report.setPainPointAnalysis("这个想法能看到真实场景，但目前更像把一组功能堆在一起，还没有把最痛的那一个用户问题钉牢。建议先回答：谁在什么高频时刻因为现有方案不够好而愿意立刻试用你。");
        report.setFakeDemandRisks(List.of(
                "把用户说的“想要更方便”直接当成强需求，但没有验证他们是否愿意迁移、付费或持续使用。",
                "需求描述偏宽，容易把低频好奇心误判成稳定使用动机。",
                "如果第一版同时照顾太多人群，会失去一个清晰的产品切口。"
        ));
        report.setFeatureRedundancyCheck(List.of(
                "第一版不需要完整社区、积分、复杂资料页或多角色后台，先证明核心评审闭环能成立。",
                "凡是不能帮助用户更快得到评审报告、保存历史或导出 Markdown 的功能，都应延后。",
                "不要把产品做成自由聊天框，报告结构才是这个 Agent 的产品形态。"
        ));
        report.setColdStartProblems(List.of(
                "冷启动的关键不是模型能说多少，而是用户是否愿意把真实产品想法交给它评审。",
                "如果没有示例输入和稳定报告结构，新用户第一次使用会不知道该提交什么。",
                "历史记录可以提升复用价值，但第一版要先让单次报告足够锋利、可执行。"
        ));
        report.setMvpSuggestions(List.of(
                "把第一版收敛为一个输入页、一个结构化结果页、一个历史记录页。",
                "固定 10 个报告模块，不开放自由问答，避免稀释产品定位。",
                "优先打磨 Mock 到真实 AI 的接口边界，确保后续接 DeepSeek 时不用重写前后端合同。"
        ));

        ReviewReportDto.MinimumBuildVersion minimumBuildVersion = new ReviewReportDto.MinimumBuildVersion();
        minimumBuildVersion.setGoal("验证“用户输入产品想法后，可以获得犀利但可执行的产品评审报告”这一核心闭环。");
        minimumBuildVersion.setCoreFeatures(List.of(
                "提交产品想法并选择评审模式、吐槽强度",
                "生成包含 10 个固定模块的结构化评审报告",
                "保存评审历史并支持查看详情",
                "输出可复制给 Codex/Cursor 的开发 Prompt"
        ));
        minimumBuildVersion.setExcludedFeatures(List.of(
                "完整登录注册",
                "支付和额度系统",
                "分享链接",
                "复杂后台管理",
                "RAG 和多 Agent 编排"
        ));
        report.setMinimumBuildVersion(minimumBuildVersion);
        report.setDeveloperPrompt(buildDeveloperPrompt(ideaName));
        return report;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-product-reviewer-v1";
    }

    private String oneLineVerdict(ReviewMode mode, int roastLevel) {
        if (mode == ReviewMode.MENTOR) {
            return "这个想法有可塑性，但第一版必须更小，先抓住一个愿意反复使用的真实场景。";
        }
        if (mode == ReviewMode.CLIENT) {
            return "能做，但现在还缺少可验收的商业结果，先把核心价值和交付边界讲清楚。";
        }
        if (roastLevel >= 3) {
            return "这个想法不是没价值，但现在有点像把愿望清单包装成产品，得先狠砍范围。";
        }
        return "这个想法有场景，但现在还像功能清单，不像一个真正的产品切口。";
    }

    private String buildDeveloperPrompt(String ideaName) {
        return """
                你是 Codex，请基于当前仓库实现 %s 的最小可运行版本。先读取 README、现有目录、接口文档和数据库脚本，确认技术栈与运行方式后再修改代码。

                范围：
                - 保留结构化产品评审报告，不做自由聊天。
                - 实现输入、生成报告、保存历史、查看详情、导出 Markdown 的闭环。
                - 不实现支付、分享链接、复杂后台、RAG 或多 Agent。

                要求：
                - 不提交任何 API Key。
                - 所有用户输入必须做长度和枚举校验。
                - 错误响应不得暴露堆栈。
                - 修改后运行可用的构建、测试或最小接口验证。
                """.formatted(ideaName);
    }

    private String summarizeIdea(String content) {
        String normalized = content == null ? "产品想法" : content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 32) {
            return normalized;
        }
        return normalized.substring(0, 32) + "...";
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
