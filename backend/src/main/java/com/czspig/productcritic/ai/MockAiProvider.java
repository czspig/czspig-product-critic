package com.czspig.productcritic.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.czspig.productcritic.common.ReviewMode;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public ReviewReportDto review(CreateReviewRequest request, String prompt) {
        ReviewMode mode = ReviewMode.requireValid(request.getMode());
        String content = normalize(request.getContent());
        IdeaProfile profile = IdeaProfile.from(content);

        ReviewReportDto report = new ReviewReportDto();
        report.setReviewTargetType(profile.reviewTargetType());
        report.setGoDecision(profile.goDecision());
        report.setBeatScore(score(profile.baseRisk() + request.getRoastLevel() * 4 + lengthRisk(content)));
        report.setPositioningScore(score(profile.baseClarity() + clarityBonus(content) - scopePenalty(content)));
        report.setOneLineVerdict(oneLineVerdict(mode, request.getRoastLevel(), profile));
        report.setGoDecisionReason(goDecisionReason(profile));
        report.setPainPointAnalysis(painPointAnalysis(profile));
        report.setFakeDemandRisks(fakeDemandRisks(profile));
        report.setFeatureRedundancyCheck(featureRedundancyCheck(profile));
        report.setColdStartProblems(coldStartProblems(profile));
        report.setMvpSuggestions(mvpSuggestions(profile));

        ReviewReportDto.MinimumBuildVersion minimum = new ReviewReportDto.MinimumBuildVersion();
        minimum.setGoal(minimumGoal(profile));
        minimum.setCoreFeatures(coreFeatures(profile));
        minimum.setExcludedFeatures(excludedFeatures(profile));
        minimum.setSuccessMetric(successMetric(profile));
        minimum.setValidationPlan(validationPlan(profile));
        report.setMinimumBuildVersion(minimum);
        report.setDeveloperPrompt(developerPrompt(profile));
        return report;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-product-reviewer-v2";
    }

    private String oneLineVerdict(ReviewMode mode, int roastLevel, IdeaProfile profile) {
        String tone = mode == ReviewMode.MENTOR
                ? "方向可以聊，但第一刀要砍得更窄："
                : roastLevel >= 3 ? "别急着写代码，先把这个坑看清楚：" : "先收敛一下：";
        return switch (profile.industry()) {
            case "campus_trade" -> tone + "校园二手交易不缺摊位，缺的是可信履约和供需密度，别把小程序做成空荡荡的跳蚤市场。";
            case "ai_resume" -> tone + "AI 简历优化的价值不在润色形容词，而在能不能让用户拿到更多面试回音。";
            case "pet_health" -> tone + "宠物健康管理不能假装自己是在线兽医，先从记录、提醒和异常分诊做低风险闭环。";
            case "mature_product" -> "这更像成熟产品定位复盘，评审只判断这段定位是否清楚，不等于否定真实产品价值。";
            case "client_requirement" -> "这是甲方需求，不是许愿池；先把验收边界钉死，再谈排期和报价。";
            default -> "输入信息还不够，像一张产品愿望便利贴，还没到能评审 MVP 的程度。";
        };
    }

    private String goDecisionReason(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "建议继续，但必须先验证一个校园、一个品类、一个履约方式；最大风险是供给不足和交易信任崩掉。";
            case "ai_resume" -> "建议调整切口，从“优化简历”收窄到“针对岗位 JD 生成可验证修改建议”；最大风险是同质化和效果不可证明。";
            case "pet_health" -> "建议谨慎继续，先避开诊断承诺；最大风险是医疗边界、数据持续录入和主人低频使用。";
            case "mature_product" -> "定位复盘可以继续，但要把目标组织、核心流程和差异化写具体，否则只能得到大而空的结论。";
            case "client_requirement" -> "先调整需求边界；当前最危险的是验收标准、数据责任和不做范围没有被明确。";
            default -> "暂停开发；当前缺少目标用户、使用场景和成功指标，继续写功能只会制造漂亮但无用的页面。";
        };
    }

    private String painPointAnalysis(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "目标用户是同校学生，真实痛点不是“想买便宜东西”这么宽，而是毕业季、换宿舍、教材换手等短窗口交易。替代方案是微信群、闲鱼和线下转让，所以产品必须证明同校信任和本地交付更省事。";
            case "ai_resume" -> "目标用户是求职者或转岗人群，痛点是简历和目标岗位不匹配、表达没有证据链、投递反馈少。替代方案是模板、朋友修改和通用 AI 聊天，差异化必须落在岗位匹配和修改理由上。";
            case "pet_health" -> "目标用户是宠物主人，痛点是疫苗驱虫提醒、症状记录和就医前信息整理。替代方案是备忘录、宠物医院微信和人工经验，产品要避免把低频焦虑包装成高频需求。";
            case "mature_product" -> "以下评审针对的是你输入的产品定位文本和从 0 复刻的可落地性，不代表对该成熟产品真实商业价值的完整评价。当前最需要补的是目标用户、关键场景和不可替代的差异化。";
            case "client_requirement" -> "甲方真正购买的是可验收的业务结果，不是功能清单。当前要先确认谁使用、谁验收、哪条主流程上线后能证明项目成功。";
            default -> "当前输入还无法判断目标用户是谁、痛点是否高频、替代方案是什么。先把“谁在什么场景下为了什么结果使用”补齐。";
        };
    }

    private List<String> fakeDemandRisks(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of(
                    "用户嘴上说需要分类、砍价、评价、担保，但第一版真正要验证的是有没有足够同校交易供给。",
                    "交易安全、纠纷处理和线下交付责任容易被低估，做不好会比微信群更麻烦。",
                    "最小实验：只选一个校区和一个高频品类，人工撮合 30 单，看成交率和复购发布率。"
            );
            case "ai_resume" -> List.of(
                    "“AI 优化”很容易变成换词工具，用户不一定愿意为泛泛润色付费。",
                    "如果不能证明投递反馈改善，所有建议都只是看起来专业。",
                    "最小实验：收集 20 份简历和目标 JD，人工交付修改前后对比，跟踪面试邀约变化。"
            );
            case "pet_health" -> List.of(
                    "宠物主人可能只在生病时想起来使用，日常记录意愿未验证。",
                    "健康建议一旦越过诊断边界，会带来信任和合规风险。",
                    "最小实验：让 15 位宠物主人连续记录 14 天，观察提醒、症状记录和就医清单哪一项真正被坚持使用。"
            );
            case "client_requirement" -> List.of(
                    "甲方说每个模块都要，但验收时通常只认业务结果，功能清单不能替代验收标准。",
                    "预算、数据提供、运营责任和上线后的维护人没有确认前，排期会失真。",
                    "最小实验：让甲方签字确认 1 条主流程、3 条验收标准和明确不做范围。"
            );
            default -> List.of(
                    "输入太抽象，无法判断用户是否真的有强动机。",
                    "当前假设还停在“有人可能需要”，缺少真实场景证据。",
                    "最小实验：补充目标用户、场景、替代方案，再找 5 个目标用户访谈。"
            );
        };
    }

    private List<String> featureRedundancyCheck(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("第一版不要做积分、复杂信用体系、多校区、物流、直播带货。", "保留发布、搜索、联系卖家、成交状态和基础举报。", "评价体系等有真实成交后再补。");
            case "ai_resume" -> List.of("第一版不要做全行业职业规划、海量模板市场、自动投递和社群。", "保留简历上传、JD 粘贴、匹配问题诊断、修改建议和前后对比。", "先证明建议质量，再谈账号体系和付费墙。");
            case "pet_health" -> List.of("第一版不要做在线诊断、问诊电商、宠物社区和复杂设备接入。", "保留宠物档案、疫苗驱虫提醒、症状日志和就医信息导出。", "任何医疗判断都要降级为提醒用户咨询兽医。");
            case "client_requirement" -> List.of("先砍掉看板、营销、复杂权限和二期愿望模块。", "只保留能支撑验收的主流程。", "没有验收人的功能不进入第一版报价。");
            default -> List.of("暂不扩功能，先补产品材料。", "不要在信息不足时做登录、支付、分享、社区或后台。", "第一版范围只能是重新提交可评审材料。");
        };
    }

    private List<String> coldStartProblems(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("冷启动靠同校密度，不靠泛流量；没有首批卖家，买家来了也会走。", "毕业季、教材季、宿舍搬迁是更好的启动窗口。", "先用社群和地推人工导入首批商品。");
            case "ai_resume" -> List.of("冷启动难点是信任：用户要相信建议真的比通用 AI 更懂岗位。", "需要拿出匿名案例和前后对比，而不是只展示炫酷编辑器。", "先从一个垂直岗位方向做样例。");
            case "pet_health" -> List.of("冷启动不是拉宠物数量，而是让主人愿意持续记录。", "如果没有提醒和就医场景触发，App 会很快沉睡。", "从宠物医院、领养社群或新手养宠群拿首批用户。");
            case "client_requirement" -> List.of("甲方项目的冷启动是上线后谁录数据、谁通知用户、谁处理异常。", "如果运营责任没人认领，系统做完也会空转。", "上线前确认初始数据、首批用户名单和运营责任人。");
            default -> List.of("无法判断第一批用户从哪里来。", "无法判断是否依赖内容、数据或供需双方。", "先补用户来源和一次完整使用链路。");
        };
    }

    private List<String> mvpSuggestions(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("只做一个校区的二手交易闭环：发布、筛选、联系、成交标记。", "先用人工审核和人工处理纠纷，别急着做复杂担保。", "用成交率、商品发布数和二次发布率验证价值。");
            case "ai_resume" -> List.of("只做“简历 + 岗位 JD”的匹配诊断和可复制修改建议。", "输出每条修改的理由、对应 JD 关键词和风险提醒。", "用用户是否采纳建议、是否继续投递和面试反馈验证。");
            case "pet_health" -> List.of("只做宠物档案、关键提醒、症状日志和就医前摘要。", "不做诊断结论，只做记录整理和提醒。", "用连续记录天数、提醒完成率和导出摘要使用率验证。");
            case "client_requirement" -> List.of("只做可验收主流程闭环。", "明确暂不做报表大屏、复杂权限、营销活动和二期模块。", "用甲方签字验收标准判断是否进入开发。");
            default -> List.of("先不开发产品，先补一版 100-200 字的产品材料。", "材料必须包含目标用户、场景、痛点、替代方案和成功指标。", "补齐后再重新评审。");
        };
    }

    private String minimumGoal(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "验证同校二手交易是否能在一个高频品类里形成真实成交。";
            case "ai_resume" -> "验证岗位定制化简历建议是否比通用润色更能被用户采纳。";
            case "pet_health" -> "验证宠物主人是否愿意持续记录并在就医前使用健康摘要。";
            case "client_requirement" -> "把甲方需求收束成可报价、可排期、可验收的最小交付范围。";
            default -> "补齐产品评审材料，让输入变得可判断。";
        };
    }

    private List<String> coreFeatures(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("商品发布", "校区和品类筛选", "卖家联系入口", "成交状态标记", "基础举报");
            case "ai_resume" -> List.of("简历文本输入", "目标 JD 输入", "匹配度诊断", "分段修改建议", "修改前后对比");
            case "pet_health" -> List.of("宠物档案", "疫苗驱虫提醒", "症状日志", "就医前摘要导出");
            case "client_requirement" -> List.of("主流程提交", "审核或处理", "结果通知", "后台查看", "验收数据留痕");
            default -> List.of("产品材料输入", "目标用户补充", "场景补充", "重新提交评审");
        };
    }

    private List<String> excludedFeatures(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("支付担保", "跨校物流", "积分等级", "直播卖货", "复杂仲裁");
            case "ai_resume" -> List.of("自动投递", "职业规划社区", "模板商城", "全行业知识库", "付费订阅墙");
            case "pet_health" -> List.of("在线诊断", "处方建议", "宠物电商", "社区信息流", "设备接入");
            case "client_requirement" -> List.of("二期愿望模块", "大屏报表", "复杂权限", "营销活动", "未定义数据迁移");
            default -> List.of("登录支付", "分享裂变", "RAG", "多 Agent", "复杂后台");
        };
    }

    private String successMetric(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "两周内同一校区发布 100 件商品，完成 30 单交易，至少 20% 卖家愿意再次发布。";
            case "ai_resume" -> "20 位用户中至少 12 位采纳修改建议，7 天后至少 5 位反馈投递回音改善。";
            case "pet_health" -> "15 位宠物主人中至少 9 位连续记录 14 天，并有 5 位导出或查看就医摘要。";
            case "client_requirement" -> "甲方确认 1 条主流程、3 条验收标准和不做范围，并能据此完成排期或报价。";
            default -> "补充后的输入能明确评审对象、目标用户、使用场景和期望结果。";
        };
    }

    private List<String> validationPlan(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("选定一个校区和一个品类。", "人工导入首批 50 件商品。", "记录浏览、咨询、成交和纠纷。", "访谈未成交用户为什么放弃。");
            case "ai_resume" -> List.of("限定一个岗位方向。", "收集 20 份真实简历和 JD。", "人工或半自动生成修改建议。", "跟踪用户采纳率和投递反馈。");
            case "pet_health" -> List.of("招募 15 位宠物主人。", "只验证提醒、日志和摘要三件事。", "连续观察 14 天记录留存。", "确认哪些内容会在就医前被使用。");
            case "client_requirement" -> List.of("召开需求边界确认会。", "写出主流程和验收标准。", "标注不做范围和甲方责任。", "再输出报价和排期。");
            default -> List.of("补充目标用户。", "补充核心场景。", "补充替代方案。", "重新提交评审。");
        };
    }

    private String developerPrompt(IdeaProfile profile) {
        return """
                你是 Codex，请基于现有仓库实现“%s”的最小可运行版本。
                目标：只验证 %s，不扩展二期功能。
                必做：%s。
                暂不做：%s。
                验收：%s
                """.formatted(
                profile.title(),
                minimumGoal(profile),
                String.join("、", coreFeatures(profile)),
                String.join("、", excludedFeatures(profile)),
                successMetric(profile)
        ).trim();
    }

    private static String normalize(String content) {
        return content == null ? "" : content.replaceAll("\\s+", " ").trim();
    }

    private int lengthRisk(String content) {
        if (content.length() < 16) {
            return 18;
        }
        if (content.length() > 160) {
            return -6;
        }
        return 0;
    }

    private int clarityBonus(String content) {
        int bonus = 0;
        for (String keyword : List.of("用户", "场景", "学校", "岗位", "JD", "宠物", "甲方", "验收", "MVP")) {
            if (content.contains(keyword)) {
                bonus += 4;
            }
        }
        return Math.min(bonus, 16);
    }

    private int scopePenalty(String content) {
        int penalty = 0;
        for (String keyword : List.of("平台", "社区", "商城", "支付", "后台", "全行业", "一站式", "生态")) {
            if (content.contains(keyword)) {
                penalty += 5;
            }
        }
        return Math.min(penalty, 20);
    }

    private int score(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record IdeaProfile(
            String title,
            String industry,
            String reviewTargetType,
            String goDecision,
            int baseRisk,
            int baseClarity) {

        static IdeaProfile from(String content) {
            String lower = content.toLowerCase(Locale.ROOT);
            if (contains(content, "甲方", "客户", "外包", "报价", "验收", "需求文档")) {
                return new IdeaProfile(title(content, "甲方需求"), "client_requirement", "CLIENT_REQUIREMENT", "PIVOT", 72, 48);
            }
            if (contains(content, "飞书", "钉钉", "企业微信", "Notion", "Slack", "微信读书")) {
                return new IdeaProfile(title(content, "成熟产品复盘"), "mature_product", "MATURE_PRODUCT", "PIVOT", 58, 56);
            }
            if (contains(content, "校园", "学生", "二手", "交易", "教材", "宿舍")) {
                return new IdeaProfile(title(content, "校园二手交易"), "campus_trade", "NEW_IDEA", "CONTINUE", 54, 68);
            }
            if (contains(content, "简历", "求职", "岗位", "面试", "招聘") || lower.contains("resume") || lower.contains("jd")) {
                return new IdeaProfile(title(content, "AI 简历优化"), "ai_resume", "NEW_IDEA", "PIVOT", 66, 62);
            }
            if (contains(content, "宠物", "猫", "狗", "疫苗", "驱虫", "健康", "兽医")) {
                return new IdeaProfile(title(content, "宠物健康管理"), "pet_health", "NEW_IDEA", "CONTINUE", 61, 59);
            }
            if (content.length() >= 18 && contains(content, "App", "APP", "小程序", "工具", "平台", "系统")) {
                return new IdeaProfile(title(content, "新产品想法"), "generic", "NEW_IDEA", "PAUSE", 78, 36);
            }
            return new IdeaProfile(title(content, "未明确输入"), "unclear", "UNCLEAR", "PAUSE", 86, 22);
        }

        private static boolean contains(String content, String... keywords) {
            List<String> matched = new ArrayList<>();
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    matched.add(keyword);
                }
            }
            return !matched.isEmpty();
        }

        private static String title(String content, String fallback) {
            if (content == null || content.isBlank()) {
                return fallback;
            }
            return content.length() <= 32 ? content : content.substring(0, 32) + "...";
        }
    }
}
