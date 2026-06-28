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
        report.setGoDecision(goDecision(request.getContent()));
        report.setGoDecisionReason(goDecisionReason(report.getGoDecision()));
        report.setBeatScore(mockBeatScore(request.getContent(), roastLevel));
        report.setPositioningScore(mockPositioningScore(request.getContent(), roastLevel));
        report.setPainPointAnalysis("目标用户看起来是有一个模糊任务要完成的早期用户，但真实痛点还没有被压缩到一个高频、刚需、强动机的时刻。现在的替代方案通常是微信群、表格、手工搜索或直接找熟人解决，所以你必须证明用户愿意离开这些低成本替代方案。");
        report.setFakeDemandRisks(List.of(
                "用户嘴上说“最好都能有”，但第一版如果没有明确触发场景，很可能只被试用一次就流失。",
                "尚未验证用户是否愿意迁移、持续提交真实内容，或为更省时间的结果付出成本。",
                "最小实验：找 5 个目标用户，用手工方式交付一次结果，观察他们是否愿意第二次主动使用。"
        ));
        report.setFeatureRedundancyCheck(List.of(
                "第一版不该做社区、积分、复杂资料页、多角色后台和泛化配置，它们只是让产品看起来完整。",
                "凡是不能帮助用户完成一次核心任务、保存结果或复用结果的功能，都应延后。",
                "第一版应该砍到一个输入、一个结构化结果、一条历史记录和一个可复制 Prompt。"
        ));
        report.setColdStartProblems(List.of(
                "第一批用户应来自你能直接触达的目标人群，例如独立开发者群、课程作业小组或真实甲方项目。",
                "如果没有内容、数据或供需双方，产品仍要能靠一次单点交付成立，否则会被冷启动拖死。",
                "最小冷启动路径：用示例和手工邀请让用户完成一次提交，再用报告质量驱动复用。"
        ));
        report.setMvpSuggestions(List.of(
                "第一版只保留输入、评审模式、生成报告、历史详情、复制 Prompt 和导出 Markdown。",
                "明确不做登录、支付、分享、RAG、多 Agent、社区和后台配置。",
                "验证方式是让 10 个目标用户提交真实想法，观察是否愿意保存报告并复制开发 Prompt。"
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
        minimumBuildVersion.setSuccessMetric("至少 10 个目标用户完成提交，其中 5 人愿意复制开发 Prompt 或基于报告调整需求范围。");
        minimumBuildVersion.setValidationPlan(List.of(
                "用 3 个典型样例跑通报告质量，确认 goDecision、风险和 MVP 建议有明显差异。",
                "邀请 5-10 个目标用户提交真实想法，记录他们是否保存报告或复制 Prompt。",
                "复盘用户最常复制的段落，把低价值模块砍掉或改写。"
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
                你是 Codex，请基于当前仓库实现“%s”的最小可运行版本。先读取 README、接口文档、入口文件和锁文件，确认技术栈、运行方式与现有组件后再修改代码。

                项目目标：
                - 让用户输入一个产品想法后，获得结构化产品评审、MVP 收缩建议和可执行开发 Prompt。

                技术栈建议：
                - 前端沿用 Vue 3、Vite、TypeScript、Pinia 和 Vue Router。
                - 后端沿用 Spring Boot、MyBatis Plus 和当前 Review API。

                页面结构：
                - 首页：输入产品想法，选择评审模式和吐槽强度，展示提交状态与摘要。
                - 详情页：展示完整报告、复制 Prompt、导出 Markdown。
                - 历史页：按匿名 session 查看过往报告。

                核心功能：
                - 生成包含 goDecision、毒打指数、定位评分、伪需求风险、冷启动问题、MVP 建议和最小可开发版本的报告。
                - 保存 review id，保证历史记录可回看同一条详情。

                不做范围：
                - 不做登录、支付、分享链接、RAG、多 Agent 或复杂后台。

                验收标准：
                - 示例输入能生成差异化报告。
                - 复制开发 Prompt、复制完整报告和导出 Markdown 可用。
                - 前端 build 通过，错误信息不泄露密钥、系统提示或堆栈。
                - 不提交任何 API Key。
                """.formatted(ideaName);
    }

    private String goDecision(String content) {
        String normalized = content == null ? "" : content;
        if (normalized.contains("甲方") || normalized.contains("公告") || normalized.contains("用户管理")) {
            return "PIVOT";
        }
        if (normalized.length() < 40 || normalized.contains("吐槽广场")) {
            return "PAUSE";
        }
        return "CONTINUE";
    }

    private String goDecisionReason(String decision) {
        return switch (decision) {
            case "CONTINUE" -> "方向值得继续，但必须先收缩到一个高频核心场景，用最小闭环验证用户是否会反复使用。";
            case "PIVOT" -> "方向有价值，但当前切入点像需求清单，交付边界和验收标准太散，需要先改成单一业务目标。";
            default -> "当前证据不足，更多是想象中的完整产品，建议先用手工实验验证真实需求再开发。";
        };
    }

    private int mockBeatScore(String content, int roastLevel) {
        int score = 52 + roastLevel * 8;
        String normalized = content == null ? "" : content;
        if (normalized.contains("社区") || normalized.contains("广场") || normalized.contains("用户管理")) {
            score += 12;
        }
        if (normalized.contains("MVP") || normalized.contains("Prompt")) {
            score -= 8;
        }
        return clamp(score);
    }

    private int mockPositioningScore(String content, int roastLevel) {
        int score = 68 - roastLevel;
        String normalized = content == null ? "" : content;
        if (normalized.contains("独立开发者") || normalized.contains("目标用户") || normalized.contains("甲方")) {
            score += 10;
        }
        if (normalized.contains("还有") || normalized.contains("包含") || normalized.contains("广场")) {
            score -= 14;
        }
        return clamp(score);
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
