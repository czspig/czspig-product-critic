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
        String reviewTargetType = inferReviewTargetType(request.getContent());
        String focus = inferFocus(request.getContent(), reviewTargetType);

        ReviewReportDto report = new ReviewReportDto();
        report.setReviewTargetType(reviewTargetType);
        report.setOneLineVerdict(oneLineVerdict(mode, roastLevel, focus, reviewTargetType));
        report.setGoDecision(goDecision(request.getContent(), reviewTargetType));
        report.setGoDecisionReason(goDecisionReason(report.getGoDecision(), focus, reviewTargetType));
        report.setBeatScore(mockBeatScore(request.getContent(), roastLevel, reviewTargetType));
        report.setPositioningScore(mockPositioningScore(request.getContent(), roastLevel, reviewTargetType));
        report.setPainPointAnalysis(painPointAnalysis(ideaName, focus, reviewTargetType));
        report.setFakeDemandRisks(fakeDemandRisks(ideaName, focus, reviewTargetType));
        report.setFeatureRedundancyCheck(featureRedundancyCheck(focus, reviewTargetType));
        report.setColdStartProblems(coldStartProblems(focus, reviewTargetType));
        report.setMvpSuggestions(mvpSuggestions(focus, reviewTargetType));

        ReviewReportDto.MinimumBuildVersion minimumBuildVersion = new ReviewReportDto.MinimumBuildVersion();
        minimumBuildVersion.setGoal(minimumGoal(ideaName, focus, reviewTargetType));
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
        minimumBuildVersion.setSuccessMetric(successMetric(focus, reviewTargetType));
        minimumBuildVersion.setValidationPlan(validationPlan(reviewTargetType));
        report.setMinimumBuildVersion(minimumBuildVersion);
        report.setDeveloperPrompt(buildDeveloperPrompt(ideaName, focus));
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

    private String oneLineVerdict(ReviewMode mode, int roastLevel, String focus, String reviewTargetType) {
        if ("MATURE_PRODUCT".equals(reviewTargetType)) {
            return "「%s」更像成熟产品定位复盘，不能按新产品创业风险一棒子打死，但这段定位文本还要讲清具体场景和差异化。".formatted(focus);
        }
        if ("CLIENT_REQUIREMENT".equals(reviewTargetType)) {
            return "「%s」不是不能接，而是现在验收边界和优先级太散，先拆清楚再报价。".formatted(focus);
        }
        if ("UNCLEAR".equals(reviewTargetType)) {
            return "这段输入还看不出是在评估新想法、成熟产品还是甲方需求，先补目标用户、场景和成功标准。";
        }
        if (mode == ReviewMode.MENTOR) {
            return "这个想法在「%s」上有可塑性，但第一版必须更小，先抓住一个愿意反复使用的真实场景。".formatted(focus);
        }
        if (mode == ReviewMode.CLIENT) {
            return "能做，但「%s」现在还缺少可验收的商业结果，先把核心价值和交付边界讲清楚。".formatted(focus);
        }
        if (roastLevel >= 3) {
            return "「%s」不是没价值，但现在有点像把愿望清单包装成产品，得先狠砍范围。".formatted(focus);
        }
        return "「%s」有场景，但现在还像功能清单，不像一个真正的产品切口。".formatted(focus);
    }

    private String buildDeveloperPrompt(String ideaName, String focus) {
        return """
                你是 Codex，请基于当前仓库实现“%s”的最小可运行版本。先读取 README、接口文档、入口文件和锁文件，确认技术栈、运行方式与现有组件后再修改代码。

                项目目标：
                - 围绕“%s”让用户输入一个产品想法后，获得结构化产品评审、MVP 收缩建议和可执行开发 Prompt。

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
                """.formatted(ideaName, focus);
    }

    private String goDecision(String content, String reviewTargetType) {
        String normalized = content == null ? "" : content;
        if ("MATURE_PRODUCT".equals(reviewTargetType)) {
            if (normalized.contains("效率") || normalized.contains("平台")) {
                return "PIVOT";
            }
            return "CONTINUE";
        }
        if ("CLIENT_REQUIREMENT".equals(reviewTargetType)) {
            return "PIVOT";
        }
        if ("UNCLEAR".equals(reviewTargetType)) {
            return "PAUSE";
        }
        if (normalized.contains("甲方") || normalized.contains("公告") || normalized.contains("用户管理")) {
            return "PIVOT";
        }
        if (normalized.length() < 40 || normalized.contains("吐槽广场")) {
            return "PAUSE";
        }
        return "CONTINUE";
    }

    private String goDecisionReason(String decision, String focus, String reviewTargetType) {
        if ("MATURE_PRODUCT".equals(reviewTargetType)) {
            String boundary = "以下评审针对的是你输入的产品定位文本和从 0 复刻的可落地性，不代表对该成熟产品真实商业价值的完整评价。";
            return switch (decision) {
                case "CONTINUE" -> "%s「%s」定位描述相对清楚，适合继续拆解其目标组织、核心场景和生态壁垒。".formatted(boundary, focus);
                case "PIVOT" -> "%s「%s」现在仍偏宏大，适合换成一个更具体的组织、场景或流程切入点再复盘。".formatted(boundary, focus);
                default -> "%s当前输入过于抽象，缺少目标用户、核心场景和差异化，暂不适合直接评审。".formatted(boundary);
            };
        }
        if ("CLIENT_REQUIREMENT".equals(reviewTargetType)) {
            return switch (decision) {
                case "CONTINUE" -> "「%s」边界较清楚，可以进入排期或报价，但仍要把验收指标写成可测试条款。".formatted(focus);
                case "PIVOT" -> "「%s」范围过大或验收模糊，直接接单会把报价、排期和责任边界都拖进泥里。".formatted(focus);
                default -> "「%s」风险过高，需求目标、验收口径和交付优先级都不清楚，不建议直接接单或开发。".formatted(focus);
            };
        }
        if ("UNCLEAR".equals(reviewTargetType)) {
            return "当前输入缺少评审对象、目标用户、使用场景和成功标准，继续评审只会产出看似完整但不可执行的建议。";
        }
        return switch (decision) {
            case "CONTINUE" -> "「%s」方向值得继续，但必须先收缩到一个高频核心场景，用最小闭环验证用户是否会反复使用。".formatted(focus);
            case "PIVOT" -> "「%s」方向有价值，但当前切入点像需求清单，交付边界和验收标准太散，需要先改成单一业务目标。".formatted(focus);
            default -> "「%s」当前证据不足，更多是想象中的完整产品，建议先用手工实验验证真实需求再开发。".formatted(focus);
        };
    }

    private int mockBeatScore(String content, int roastLevel, String reviewTargetType) {
        int score = switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> 54;
            case "CLIENT_REQUIREMENT" -> 68;
            case "UNCLEAR" -> 82;
            default -> 52 + roastLevel * 8;
        };
        String normalized = content == null ? "" : content;
        if (normalized.contains("社区") || normalized.contains("广场") || normalized.contains("用户管理")) {
            score += 12;
        }
        if (normalized.contains("飞书") || normalized.contains("钉钉") || normalized.contains("企业微信")) {
            score += 8;
        }
        if (normalized.contains("MVP") || normalized.contains("Prompt")) {
            score -= 8;
        }
        return clamp(score);
    }

    private int mockPositioningScore(String content, int roastLevel, String reviewTargetType) {
        int score = switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> 58;
            case "CLIENT_REQUIREMENT" -> 50;
            case "UNCLEAR" -> 24;
            default -> 68 - roastLevel;
        };
        String normalized = content == null ? "" : content;
        if (normalized.contains("独立开发者") || normalized.contains("目标用户") || normalized.contains("甲方")) {
            score += 10;
        }
        if (normalized.contains("飞书") || normalized.contains("钉钉") || normalized.contains("企业微信")) {
            score += 6;
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

    private String inferReviewTargetType(String content) {
        String normalized = content == null ? "" : content;
        if (containsAny(normalized, "飞书", "钉钉", "企业微信", "协作", "办公")) {
            return "MATURE_PRODUCT";
        }
        if (containsAny(normalized, "甲方", "客户", "报价", "验收", "外包")) {
            return "CLIENT_REQUIREMENT";
        }
        if (containsAny(normalized, "我想做", "小程序", "App", "APP", "工具")) {
            return "NEW_IDEA";
        }
        return "UNCLEAR";
    }

    private String inferFocus(String content, String reviewTargetType) {
        String normalized = content == null ? "" : content;
        if ("MATURE_PRODUCT".equals(reviewTargetType)) {
            if (containsAny(normalized, "飞书", "协作", "办公")) {
                return "企业协作办公定位";
            }
            return "成熟产品定位文本";
        }
        if ("CLIENT_REQUIREMENT".equals(reviewTargetType) || normalized.contains("甲方") || normalized.contains("验收") || normalized.contains("公告")) {
            return "甲方交付和验收边界";
        }
        if (normalized.contains("独立开发者") || normalized.contains("Prompt") || normalized.contains("MVP")) {
            return "独立开发者的需求收缩";
        }
        if (normalized.contains("社区") || normalized.contains("广场") || normalized.contains("内容")) {
            return "内容冷启动和复用";
        }
        if (normalized.contains("用户管理") || normalized.contains("后台")) {
            return "后台管理和权限边界";
        }
        return summarizeIdea(normalized);
    }

    private String painPointAnalysis(String ideaName, String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> "以下评审针对的是你输入的产品定位文本和从 0 复刻的可落地性，不代表对该成熟产品真实商业价值的完整评价。Mock 识别到输入「%s」属于%s：要复盘的不是产品成败，而是定位文本是否讲清目标组织、核心协作场景、关键差异化和生态壁垒。如果只是“提升企业协作效率”，痛点仍过抽象。".formatted(ideaName, focus);
            case "CLIENT_REQUIREMENT" -> "Mock 识别到输入「%s」属于%s：核心痛点不是能不能开发，而是甲方到底要哪个业务结果、谁验收、按什么标准验收。当前最大矛盾是模块很多，但成功标准不够具体。".formatted(ideaName, focus);
            case "UNCLEAR" -> "Mock 识别到输入「%s」信息不足：暂时无法判断用户、场景、痛点和目标结果。当前最大矛盾是想被评审，但还没有给出可评审的产品材料。".formatted(ideaName);
            default -> "Mock 评审基于输入「%s」判断：当前重点像是%s，但真实痛点还没有被压缩到一个高频、刚需、强动机的时刻。现在的替代方案通常是微信群、表格、手工搜索或直接找熟人解决，所以你必须证明用户愿意为了这个场景离开低成本替代方案。".formatted(ideaName, focus);
        };
    }

    private List<String> fakeDemandRisks(String ideaName, String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> List.of(
                    "成熟产品复盘不要把「%s」等同于新团队从 0 立刻能做，组织协同、生态集成和销售服务都可能是壁垒。".formatted(focus),
                    "如果输入只停留在“协作办公平台”，定位文本缺少目标组织、关键流程和差异化，不足以指导 MVP。",
                    "最小验证：把「%s」拆成一个具体团队、一个高频流程和一个不可替代的协作痛点，再判断是否值得从 0 学习或复刻。".formatted(ideaName)
            );
            case "CLIENT_REQUIREMENT" -> List.of(
                    "甲方可能说每个模块都要，但真正验收时只认业务结果，范围清单不能替代验收标准。",
                    "尚未验证预算、排期、数据权限、内容维护责任和上线后的运营人是谁。",
                    "最小实验：先让甲方确认 1 个最高优先级业务目标，并写出 3 条可测试验收标准。"
            );
            case "UNCLEAR" -> List.of(
                    "输入缺少目标用户，无法判断谁会用。",
                    "输入缺少使用场景，无法判断痛点频率和强度。",
                    "最小动作：补充“谁在什么场景下为了什么结果使用”，再进入评审。"
            );
            default -> List.of(
                    "围绕「%s」的用户可能嘴上说“最好都能有”，但第一版如果没有明确触发场景，很可能只被试用一次就流失。".formatted(focus),
                    "尚未验证用户是否愿意迁移、持续提交真实内容，或为更省时间的结果付出成本。",
                    "最小实验：找 5 个目标用户，用手工方式交付一次「%s」相关结果，观察他们是否愿意第二次主动使用。".formatted(ideaName)
            );
        };
    }

    private List<String> featureRedundancyCheck(String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> List.of(
                    "第一版不要照搬成熟产品的通讯录、权限、文档、会议、审批和生态集成全家桶。",
                    "优先拆「%s」里最能体现差异化的一个核心流程，而不是复刻完整平台。".formatted(focus),
                    "成熟组织能力和生态壁垒应作为复盘对象，不应作为 MVP 第一版功能清单。"
            );
            case "CLIENT_REQUIREMENT" -> List.of(
                    "先砍掉“看起来完整”的后台、统计、宣传、社区等泛模块，保留能支撑验收的主流程。",
                    "没有验收人、验收时间和验收口径的功能，第一版都不应进入报价。",
                    "把「%s」拆成 must-have、should-have 和暂不做三档。".formatted(focus)
            );
            case "UNCLEAR" -> List.of(
                    "暂不讨论功能清单，先补清评审对象。",
                    "不要在信息不足时直接扩展登录、支付、分享、RAG 或多 Agent。",
                    "第一版范围只能是补充材料和重新提交评审。"
            );
            default -> List.of(
                    "第一版不该为了「%s」过早做社区、积分、复杂资料页、多角色后台和泛化配置，它们只是让产品看起来完整。".formatted(focus),
                    "凡是不能帮助用户完成一次核心任务、保存结果或复用结果的功能，都应延后。",
                    "第一版应该砍到一个输入、一个结构化结果、一条历史记录和一个可复制 Prompt。"
            );
        };
    }

    private List<String> coldStartProblems(String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> List.of(
                    "成熟产品的冷启动往往依赖组织导入、管理制度、生态迁移和销售服务，新团队不能假设这些能力天然存在。",
                    "如果从 0 做「%s」，第一批用户必须来自一个可控组织或单一流程，而不是泛泛企业市场。".formatted(focus),
                    "最小冷启动路径：找一个 5-20 人团队，只验证一个协作流程是否比现有工具更省事。"
            );
            case "CLIENT_REQUIREMENT" -> List.of(
                    "甲方项目的冷启动不是拉用户，而是上线后谁录数据、谁运营、谁处理异常。",
                    "如果甲方不能提供内容、账号、审核和推广资源，系统做完也可能空转。",
                    "最小冷启动路径：上线前确认首批用户名单、初始数据来源和运营责任人。"
            );
            case "UNCLEAR" -> List.of(
                    "无法判断第一批用户从哪里来。",
                    "无法判断是否需要内容、数据或供需双方。",
                    "先补用户来源和一次完整使用链路。"
            );
            default -> List.of(
                    "第一批用户应来自你能直接触达且真的关心「%s」的人群，例如独立开发者群、课程作业小组或真实甲方项目。".formatted(focus),
                    "如果没有内容、数据或供需双方，产品仍要能靠一次单点交付成立，否则会被冷启动拖死。",
                    "最小冷启动路径：用示例和手工邀请让用户完成一次提交，再用报告质量驱动复用。"
            );
        };
    }

    private List<String> mvpSuggestions(String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> List.of(
                    "把「%s」收缩成一个目标组织、一个核心场景、一个关键差异化，不要复刻整个平台。".formatted(focus),
                    "明确不做企业通讯录全量权限、开放平台、复杂审批、生态集成和大规模商业化能力。",
                    "验证方式是访谈 3 个目标团队，确认这个单点场景是否比现有工具更清楚、更省协作成本。"
            );
            case "CLIENT_REQUIREMENT" -> List.of(
                    "先做能验收的主流程闭环，例如提交、审核、结果通知和后台查看。",
                    "明确不做范围外模块、二期愿望、复杂数据看板、营销活动和未定义权限。",
                    "验证方式是让甲方签字确认需求优先级、验收样例和不做范围。"
            );
            case "UNCLEAR" -> List.of(
                    "先补充目标用户、使用场景、核心痛点和期望结果。",
                    "暂不进入开发，不做功能扩写。",
                    "验证方式是重写一段 100-200 字的产品材料后重新评审。"
            );
            default -> List.of(
                    "第一版只保留能验证「%s」的输入、评审模式、生成报告、历史详情、复制 Prompt 和导出 Markdown。".formatted(focus),
                    "明确不做登录、支付、分享、RAG、多 Agent、社区和后台配置。",
                    "验证方式是让 10 个目标用户提交真实想法，观察是否愿意保存报告并复制开发 Prompt。"
            );
        };
    }

    private String minimumGoal(String ideaName, String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> "验证「%s」能否被拆成一个可从 0 学习或复刻的最小定位切入点。".formatted(focus);
            case "CLIENT_REQUIREMENT" -> "把「%s」收束成可报价、可排期、可验收的最小交付范围。".formatted(focus);
            case "UNCLEAR" -> "补齐「%s」的评审材料，先让输入变得可判断。".formatted(ideaName);
            default -> "验证「%s」能否通过一次提交获得可执行反馈这一核心闭环。".formatted(ideaName);
        };
    }

    private String successMetric(String focus, String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> "能把「%s」写成 1 个目标组织、1 个核心场景、1 个差异化判断和 1 个从 0 切入的 MVP。".formatted(focus);
            case "CLIENT_REQUIREMENT" -> "甲方确认 1 个主流程、3 条验收标准、明确不做范围，并据此完成报价或排期。";
            case "UNCLEAR" -> "补充后的输入能明确评审对象、目标用户、使用场景和期望结果。";
            default -> "至少 10 个目标用户围绕「%s」完成提交，其中 5 人愿意复制开发 Prompt 或基于报告调整需求范围。".formatted(focus);
        };
    }

    private List<String> validationPlan(String reviewTargetType) {
        return switch (reviewTargetType) {
            case "MATURE_PRODUCT" -> List.of(
                    "把成熟产品描述改写成具体定位文本，避免只写平台愿景。",
                    "列出 3 个成熟组织或生态壁垒，标记哪些不适合 MVP 第一版。",
                    "选择 1 个可从 0 验证的单点场景，访谈 3 个目标团队。"
            );
            case "CLIENT_REQUIREMENT" -> List.of(
                    "和甲方确认首期业务目标，只保留一个主流程。",
                    "把验收标准写成可测试清单，并确认不做范围。",
                    "基于确认后的范围再估算排期、报价和风险。"
            );
            case "UNCLEAR" -> List.of(
                    "补充目标用户、场景、痛点、现有替代方案。",
                    "说明这是新产品想法、成熟产品复盘还是甲方需求。",
                    "重新提交后再进入完整评审。"
            );
            default -> List.of(
                    "用 3 个典型样例跑通报告质量，确认 goDecision、风险和 MVP 建议有明显差异。",
                    "邀请 5-10 个目标用户提交真实想法，记录他们是否保存报告或复制 Prompt。",
                    "复盘用户最常复制的段落，把低价值模块砍掉或改写。"
            );
        };
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
