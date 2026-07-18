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
        int roastLevel = request.getRoastLevel() == null ? 2 : request.getRoastLevel();
        String content = normalize(request.getContent());
        IdeaProfile profile = IdeaProfile.from(content);

        ReviewReportDto report = new ReviewReportDto();
        report.setReviewTargetType(profile.reviewTargetType());
        report.setGoDecision(profile.goDecision());
        report.setBeatScore(score(profile.baseRisk() + roastLevel * 4 + lengthRisk(content)));
        report.setPositioningScore(score(profile.baseClarity() + clarityBonus(content) - scopePenalty(content)));
        report.setOneLineVerdict(oneLineVerdict(mode, roastLevel, profile));
        report.setGoDecisionReason(applyTone(roastLevel, goDecisionReason(profile)));
        report.setPainPointAnalysis(applyTone(roastLevel, painPointAnalysis(profile)));
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
        report.setDimensionScores(dimensionScores(report, profile, content));
        report.setKnowledgeBoundary(knowledgeBoundary(profile, content));
        report.setOverallAssessment(overallAssessment(report, profile));
        report.setQualityAudit(initialQualityAudit());
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
        String tone = mode == ReviewMode.MENTOR || roastLevel <= 1
                ? "方向可以聊，但第一刀要砍得更窄："
                : roastLevel >= 3 ? "地狱模式直说：别急着写代码，先把这个坑看清楚：" : "犀利模式先收拾一下：";
        return switch (profile.industry()) {
            case "campus_trade" -> tone + "校园二手交易不缺摊位，缺的是可信履约和供需密度，别把小程序做成空荡荡的跳蚤市场。";
            case "ai_resume" -> tone + "AI 简历优化的价值不在润色形容词，而在能不能让用户拿到更多面试回音。";
            case "pet_health" -> tone + "宠物健康管理不能假装自己是在线兽医，先从记录、提醒和异常分诊做低风险闭环。";
            case "desktop_tool" -> tone + "桌面工具的价值不在角色可爱，而在它能不能比系统通知和备忘录更稳定地帮用户处理真实任务。";
            case "mature_product" -> "这更像成熟产品定位复盘，评审只判断这段定位是否清楚，不等于否定真实产品价值。";
            case "client_requirement" -> "这是甲方需求，不是许愿池；先把验收边界钉死，再谈排期和报价。";
            case "generic" -> tone + "这不是固定模板题，先按你给出的目标用户、场景和商业模式做小范围验证，别一上来把产品做成全功能展厅。";
            default -> "输入信息还不太够，像一张产品愿望便利贴，还没到能评审 MVP 的程度。";
        };
    }

    private String applyTone(int roastLevel, String text) {
        if (roastLevel <= 1) {
            return "温和模式提醒：" + text;
        }
        if (roastLevel >= 3) {
            return "地狱模式直说：" + text + " 这不是否定你，是防止你把时间烧在一个没人买单的漂亮页面上。";
        }
        return "犀利模式判断：" + text;
    }

    private String goDecisionReason(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "建议继续，但必须先验证一个校园、一个品类、一个履约方式；最大风险是供给不足和交易信任塌掉。";
            case "ai_resume" -> "建议调整切口，从“优化简历”收窄到“针对岗位 JD 生成可验证修改建议”；最大风险是同质化和效果不可证明。";
            case "pet_health" -> "建议谨慎继续，先避开诊断承诺；最大风险是医疗边界、数据持续录入和主人低频使用。";
            case "desktop_tool" -> "建议谨慎验证；最大风险是用户只是觉得桌宠有趣，但不会长期授权通知、保持常驻或依赖它处理待办。";
            case "mature_product" -> "定位复盘可以继续，但要把目标组织、核心流程和差异化写具体，否则只能得到大而空的结论。";
            case "client_requirement" -> "先调整需求边界；当前最危险的是验收标准、数据责任和不做范围没有被明确。";
            case "generic" -> genericDecisionReason(profile);
            default -> "暂停开发；当前缺少足够产品事实，继续写功能只会制造漂亮但无用的页面。";
        };
    }

    private String genericDecisionReason(IdeaProfile profile) {
        List<String> missing = profile.missingInputParts();
        if (missing.isEmpty()) {
            return "建议谨慎验证；输入已经覆盖关键评审材料，但仍要先用小样本证明真实使用动机、买单意愿和最小闭环。";
        }
        return "建议先补齐材料；当前还缺少" + String.join("、", missing) + "，继续开发会让风险藏在漂亮页面后面。";
    }

    private String painPointAnalysis(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "目标用户是同校学生，真实痛点不是“想买便宜东西”这么宽，而是毕业季、换宿舍、教材换手等短窗口交易。替代方案是微信群、闲鱼和线下转让，所以产品必须证明同校信任和本地交付更省事。";
            case "ai_resume" -> "目标用户是求职者或转岗人群，痛点是简历和目标岗位不匹配、表达没有证据链、投递反馈少。替代方案是模板、朋友修改和通用 AI 聊天，差异化必须落在岗位匹配和修改理由上。";
            case "pet_health" -> "目标用户是宠物主人，痛点是疫苗驱虫提醒、症状记录和就医前信息整理。替代方案是备忘录、宠物医院微信和人工经验，产品要避免把低频焦虑包装成高频需求。";
            case "desktop_tool" -> "目标用户更像需要跨浏览器、桌面和系统通知统一处理提醒的人。真实痛点不是“想要一个可爱桌宠”，而是通知分散、过期信息重复打扰、备忘录和天气等轻任务入口割裂。替代方案是系统通知中心、浏览器扩展、便签软件和托盘工具。";
            case "mature_product" -> "以下评审针对的是你输入的产品定位文本和从 0 复刻的可落地性，不代表对该成熟产品真实商业价值的完整评价。当前最需要补的是目标用户、关键场景和不可替代的差异化。";
            case "client_requirement" -> "甲方真正购买的是可验收的业务结果，不是功能清单。当前要先确认谁使用、谁验收、哪条主流程上线后能证明项目成功。";
            case "generic" -> "当前输入已经进入可评审范围，后续重点不是继续堆功能，而是确认目标用户是否真有这个痛点、现有方案为什么不够用、第一版是否能在一个具体场景里完成闭环。";
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
            case "desktop_tool" -> List.of(
                    "桌宠外观会带来第一眼兴趣，但留存取决于通知处理是否真的省心。",
                    "浏览器插件、WebSocket、Electron 常驻和本地 JSON 存储链路较长，稳定性会直接决定用户信任。",
                    "最小实验：让 10 位重度桌面用户连续使用 7 天，记录通知处理次数、误提醒次数和主动打开面板次数。"
            );
            case "client_requirement" -> List.of(
                    "甲方说每个模块都要，但验收时通常只认业务结果，功能清单不能替代验收标准。",
                    "预算、数据提供、运营责任和上线后的维护人没有确认前，排期会失真。",
                    "最小实验：让甲方签字确认 1 条主流程、5 条验收标准和明确不做范围。"
            );
            case "generic" -> List.of(
                    "目标用户、场景和商业模式已经出现，但还不能直接等于真实需求成立。",
                    "最大伪需求风险是用户觉得“有用”，却不愿意持续使用、付费或迁移现有流程。",
                    "最小实验：用人工或半自动方式服务 5-10 位目标用户，观察是否愿意完成核心动作。"
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
            case "ai_resume" -> List.of("第一版不要做全行业职业规划、海量模板市场、自动投递和社区。", "保留简历上传、JD 粘贴、匹配问题诊断、修改建议和前后对比。", "先证明建议质量，再谈账号体系和付费墙。");
            case "pet_health" -> List.of("第一版不要做在线诊断、问诊电商、宠物社区和复杂设备接入。", "保留宠物档案、疫苗驱虫提醒、症状日志和就医信息导出。", "任何医疗判断都要降级为提醒用户咨询兽医。");
            case "desktop_tool" -> List.of("第一版不要做复杂角色养成、插件市场、多账号同步和云端社交。", "保留通知采集、桌宠提示、通知面板、去重归档和一个轻量备忘录。", "天气和外观自定义可以保留，但不要压过通知处理主链路。");
            case "client_requirement" -> List.of("先砍掉看板、营销、复杂权限和二期愿望模块。", "只保留能支撑验收的主流程。", "没有验收人的功能不进入第一版报价。");
            case "generic" -> List.of("第一版不要同时做平台化、复杂后台、社区、支付和自动化全流程。", "保留一个能交付核心价值的最小闭环。", "所有锦上添花的功能等验证后再进入排期。");
            default -> List.of("暂不扩功能，先补产品材料。", "不要在信息不足时做登录、支付、分享、社区或后台。", "第一版范围只能是重新提交可评审材料。");
        };
    }

    private List<String> coldStartProblems(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("冷启动靠同校密度，不靠泛流量；没有首批卖家，买家来了也会走。", "毕业季、教材季、宿舍搬迁是更好的启动窗口。", "先用社群和地推人工导入首批商品。");
            case "ai_resume" -> List.of("冷启动难点是信任：用户要相信建议真的比通用 AI 更懂岗位。", "需要拿出匿名案例和前后对比，而不是只展示炫酷编辑器。", "先从一个垂直岗位方向做样例。");
            case "pet_health" -> List.of("冷启动不是拉宠物数量，而是让主人愿意持续记录。", "如果没有提醒和就医场景触发，App 会很快沉睡。", "从宠物医院、领养社群或新手养宠群拿首批用户。");
            case "desktop_tool" -> List.of("冷启动要靠真实通知流，而不是靠桌宠截图传播。", "浏览器插件权限、桌面常驻和误提醒都会影响第一批用户信任。", "先从经常被网页通知和本地待办打扰的开发者或重度办公用户验证。");
            case "client_requirement" -> List.of("甲方项目的冷启动是上线后谁录数据、谁通知用户、谁处理异常。", "如果运营责任没人认领，系统做完也会空转。", "上线前确认初始数据、首批用户名单和运营责任人。");
            case "generic" -> List.of("冷启动先找输入里描述的那一类目标用户，不要泛化到所有人。", "第一批用户需要来自现有关系、垂直社群或线下试点，而不是等自然流量。", "先验证一次完整使用链路，再考虑规模化获客。");
            default -> List.of("无法判断第一批用户从哪里来。", "无法判断是否依赖内容、数据或供需双方。", "先补用户来源和一次完整使用链路。");
        };
    }

    private List<String> mvpSuggestions(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("只做一个校区的二手交易闭环：发布、筛选、联系、成交标记。", "先用人工审核和人工处理纠纷，别急着做复杂担保。", "用成交率、商品发布数和二次发布率验证价值。");
            case "ai_resume" -> List.of("只做“简历 + 岗位 JD”的匹配诊断和可复制修改建议。", "输出每条修改的理由、对应 JD 关键词和风险提醒。", "用用户是否采纳建议、是否继续投递和面试反馈验证。");
            case "pet_health" -> List.of("只做宠物档案、关键提醒、症状日志和就医前摘要。", "不做诊断结论，只做记录整理和提醒。", "用连续记录天数、提醒完成率和导出摘要使用率验证。");
            case "desktop_tool" -> List.of("只做浏览器通知采集到桌宠提醒再到面板归档的闭环。", "先验证 WebSocket 到 Electron 的稳定性、误提醒率和去重归档。", "桌宠表情、天气、备忘录保留最小版本，不要扩成大杂烩。");
            case "client_requirement" -> List.of("只做可验收主流程闭环。", "明确暂不做报表大屏、复杂权限、营销活动和二期模块。", "用甲方签字验收标准判断是否进入开发。");
            case "generic" -> List.of("只做一个目标用户在一个核心场景里的完整闭环。", "先用人工或半自动方案验证价值，不急着做完整自动化。", "用继续使用意愿、付费意愿和迁移成本接受度判断是否进入开发。");
            default -> List.of("先不开发产品，先补一版 100-200 字的产品材料。", "材料必须包含目标用户、场景、痛点、替代方案和成功指标。", "补齐后再重新评审。");
        };
    }

    private String minimumGoal(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "验证同校二手交易是否能在一个高频品类里形成真实成交。";
            case "ai_resume" -> "验证岗位定制化简历建议是否比通用润色更能被用户采纳。";
            case "pet_health" -> "验证宠物主人是否愿意持续记录并在就医前使用健康摘要。";
            case "desktop_tool" -> "验证桌面桌宠是否能稳定承接浏览器通知，并减少用户处理通知和待办的负担。";
            case "client_requirement" -> "把甲方需求收束成可报价、可排期、可验收的最小交付范围。";
            case "generic" -> "验证目标用户是否愿意在具体场景里持续使用这个最小解决方案。";
            default -> "补齐产品评审材料，让输入变得可判断。";
        };
    }

    private List<String> coreFeatures(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("商品发布", "校区和品类筛选", "卖家联系入口", "成交状态标记", "基础举报");
            case "ai_resume" -> List.of("简历文本输入", "目标 JD 输入", "匹配度诊断", "分段修改建议", "修改前后对比");
            case "pet_health" -> List.of("宠物档案", "疫苗驱虫提醒", "症状日志", "就医前摘要导出");
            case "desktop_tool" -> List.of("浏览器插件采集", "WebSocket 通道", "桌宠提醒", "通知面板", "去重归档");
            case "client_requirement" -> List.of("主流程提交", "审核或处理", "结果通知", "后台查看", "验收数据留痕");
            case "generic" -> List.of("目标用户入口", "核心信息录入", "一次核心价值交付", "结果反馈记录", "验证数据看板");
            default -> List.of("产品材料输入", "目标用户补充", "场景补充", "重新提交评审");
        };
    }

    private List<String> excludedFeatures(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("支付担保", "跨校物流", "积分等级", "直播卖货", "复杂仲裁");
            case "ai_resume" -> List.of("自动投递", "职业规划社区", "模板商城", "全行业知识库", "付费订阅墙");
            case "pet_health" -> List.of("在线诊断", "处方建议", "宠物电商", "社区信息流", "设备接入");
            case "desktop_tool" -> List.of("角色养成系统", "云端同步", "插件市场", "复杂主题商城", "社交排行榜");
            case "client_requirement" -> List.of("二期愿望模块", "大屏报表", "复杂权限", "营销活动", "未定义数据迁移");
            case "generic" -> List.of("复杂权限", "支付闭环", "社区内容", "多端同步", "全自动决策");
            default -> List.of("登录支付", "分享裂变", "RAG", "多 Agent", "复杂后台");
        };
    }

    private String successMetric(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> "两周内同一校区发布 100 件商品，完成 30 单交易，至少 20% 卖家愿意再次发布。";
            case "ai_resume" -> "20 位用户中至少 12 位采纳修改建议，7 天后至少 5 位反馈投递回音改善。";
            case "pet_health" -> "15 位宠物主人中至少 9 位连续记录 14 天，并有 5 位导出或查看就医摘要。";
            case "desktop_tool" -> "10 位用户连续使用 7 天，至少 6 位每天处理 5 条以上通知，误提醒率低于 10%。";
            case "client_requirement" -> "甲方确认 1 条主流程、5 条验收标准和不做范围，并能据此完成排期或报价。";
            case "generic" -> "首批 5-10 位目标用户完成一次核心场景验证，至少 3 位明确愿意继续使用、付费或投入迁移成本。";
            default -> "补充后的输入能明确评审对象、目标用户、使用场景和期望结果。";
        };
    }

    private List<String> validationPlan(IdeaProfile profile) {
        return switch (profile.industry()) {
            case "campus_trade" -> List.of("选定一个校区和一个品类。", "人工导入首批 50 件商品。", "记录浏览、咨询、成交和纠纷。", "访谈未成交用户为什么放弃。");
            case "ai_resume" -> List.of("限定一个岗位方向。", "收集 20 份真实简历和 JD。", "人工或半自动生成修改建议。", "跟踪用户采纳率和投递反馈。");
            case "pet_health" -> List.of("招募 15 位宠物主人。", "只验证提醒、日志和摘要三件事。", "连续观察 14 天记录留存。", "确认哪些内容会在就医前被使用。");
            case "desktop_tool" -> List.of("找 10 位重度桌面用户安装插件和桌面端。", "连续记录 7 天通知采集、点击和归档行为。", "统计误提醒、重复提醒和主动打开面板次数。", "访谈用户是否愿意保留开机自启。");
            case "client_requirement" -> List.of("召开需求边界确认会。", "写出主流程和验收标准。", "标注不做范围和甲方责任。", "再输出报价和排期。");
            case "generic" -> genericValidationPlan(profile);
            default -> List.of("补充目标用户。", "补充核心场景。", "补充替代方案。", "重新提交评审。");
        };
    }

    private List<String> genericValidationPlan(IdeaProfile profile) {
        List<String> plan = new ArrayList<>();
        if (!profile.hasTargetUser()) {
            plan.add("补充目标用户。");
        }
        if (!profile.hasScene()) {
            plan.add("补充核心场景。");
        }
        if (!profile.hasDifferentiation()) {
            plan.add("补充替代方案和差异化。");
        }
        if (!profile.hasBusinessModel()) {
            plan.add("补充商业模式或买单方假设。");
        }
        if (!profile.hasValidation()) {
            plan.add("设计一次小样本验证。");
        }
        if (plan.isEmpty()) {
            plan.add("招募 5-10 位目标用户做小样本访谈。");
            plan.add("用人工或半自动方式交付一次核心价值。");
            plan.add("记录使用完成率、继续使用意愿和付费意愿。");
            plan.add("根据负反馈收缩第一版功能范围。");
        } else {
            plan.add("补齐后重新提交评审。");
        }
        return plan;
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

    private List<ReviewReportDto.DimensionScore> dimensionScores(
            ReviewReportDto report,
            IdeaProfile profile,
            String content) {
        int clarity = report.getPositioningScore();
        int risk = report.getBeatScore();
        boolean hasUser = containsAny(content, "目标用户", "用户", "学生", "求职者", "宠物主人", "甲方", "客户");
        boolean hasScene = containsAny(content, "场景", "毕业季", "投递", "就医", "通知", "验收", "截止");
        boolean hasBusiness = containsAny(content, "商业模式", "付费", "收费", "订阅", "采购", "广告", "收入");
        boolean hasDifferentiation = containsAny(content, "不同", "差异", "替代", "比", "微信", "闲鱼", "现有方案");
        boolean hasValidation = containsAny(content, "验证", "测试", "访谈", "数据", "指标", "试点");

        return List.of(
                dimension("painPoint", "用户痛点", score(clarity + (hasScene ? 8 : -8)), confidence(hasUser && hasScene, hasUser || hasScene),
                        "HIGHER_IS_BETTER", "痛点是否来自具体人群和具体场景。",
                        evidence(content, hasUser ? "输入里已经出现目标用户线索。" : "目标用户仍不够明确。", hasScene ? "输入里出现了使用场景或触发时刻。" : "使用场景仍偏泛。"),
                        List.of(hasScene ? "尚未证明该场景发生频率。" : "缺少可观察的高频场景。", "替代方案是否足够痛仍需验证。"),
                        List.of("目标用户最近一次遇到该问题的时间", "当前替代方案和不满意原因"),
                        List.of("访谈 10 位目标用户，记录最近一次痛点发生过程", "用手工方案验证至少 5 人愿意完成核心动作")),
                dimension("marketSpace", "市场空间", score(clarity - 8 + (hasValidation ? 8 : 0)), confidence(hasValidation, hasUser),
                        "HIGHER_IS_BETTER", "市场空间暂按目标人群清晰度和可触达性判断。",
                        evidence(content, hasUser ? "输入已限定部分人群，便于估算可触达范围。" : "输入尚未限定足够窄的人群。"),
                        List.of("没有真实用户规模或触达渠道数据。", "不应把口号式市场规模当成事实。"),
                        List.of("首批用户来源", "目标人群规模的可验证口径"),
                        List.of("先定义一个学校、岗位或垂直圈层作为最小市场", "用报名、访问或访谈数据验证触达成本")),
                dimension("businessModel", "商业模式", score(clarity - (hasBusiness ? 4 : 20)), confidence(hasBusiness && hasValidation, hasBusiness),
                        "HIGHER_IS_BETTER", hasBusiness ? "已有初步付费或收入假设，但仍需验证谁买单。" : "商业模式缺口明显，不能默认有人付费。",
                        evidence(content, hasBusiness ? "输入提到了付费、采购、广告或收入线索。" : "输入没有提供明确付费方。"),
                        List.of("没有证明付费意愿。", "没有说明付费方和使用方是否一致。"),
                        List.of("谁付款", "为什么现在愿意付款", "可接受价格或采购流程"),
                        List.of("做 5 次付费意愿访谈", "用预售、试点或签字确认验证买单方")),
                dimension("implementationDifficulty", "实现难度", score(100 - clarity + scopePenalty(content)), "MEDIUM",
                        "HIGHER_IS_WORSE", "分数越高代表实现越困难，主要受范围和链路复杂度影响。",
                        evidence(content, "根据输入中的功能范围和技术链路估计。"),
                        List.of("第一版若同时做平台、社区、支付或复杂后台，难度会快速上升。"),
                        List.of("必须保留的单一核心闭环", "暂不做范围清单"),
                        List.of("把 MVP 收缩到 3-5 个核心功能", "先用低代码或人工流程验证需求")),
                dimension("growthPotential", "增长潜力", score(clarity + (hasDifferentiation ? 8 : -8)), confidence(hasDifferentiation && hasValidation, hasDifferentiation),
                        "HIGHER_IS_BETTER", "增长潜力取决于差异化是否能带来自传播或复用。",
                        evidence(content, hasDifferentiation ? "输入提到了替代方案或差异化线索。" : "输入缺少与现有方案的明确不同。"),
                        List.of("冷启动渠道未充分证明。", "复用和推荐机制仍是推测。"),
                        List.of("用户为什么愿意迁移", "首批用户如何带来第二批用户"),
                        List.of("用小范围试点观察复用率", "记录用户是否愿意推荐给同类人群")),
                dimension("riskLevel", "风险等级", risk, "MEDIUM",
                        "HIGHER_IS_WORSE", "分数越高代表风险越严重，需要优先验证或降级。",
                        evidence(content, "风险来自定位不清、冷启动、商业模式、合规或实现范围。"),
                        List.of("高风险项不能靠页面设计掩盖。", "需要先验证关键假设再扩大开发。"),
                        List.of("最高风险假设", "失败后是否还有可转向切口"),
                        List.of("列出 3 个会让项目暂停的否定信号", "先验证最高风险假设")));
    }

    private ReviewReportDto.DimensionScore dimension(
            String key,
            String name,
            int score,
            String confidence,
            String direction,
            String summary,
            List<String> evidence,
            List<String> deductions,
            List<String> missing,
            List<String> actions) {
        ReviewReportDto.DimensionScore item = new ReviewReportDto.DimensionScore();
        item.setDimensionKey(key);
        item.setDimensionName(name);
        item.setScore(score);
        item.setConfidence(confidence);
        item.setScoreDirection(direction);
        item.setSummary(summary);
        item.setEvidence(evidence);
        item.setDeductions(deductions);
        item.setMissingInformation(missing);
        item.setImprovementActions(actions);
        item.setChangeReason("Mock 根据输入信息完整度和既有报告字段生成，用于本地开发演示。");
        return item;
    }

    private ReviewReportDto.KnowledgeBoundary knowledgeBoundary(IdeaProfile profile, String content) {
        ReviewReportDto.KnowledgeBoundary boundary = new ReviewReportDto.KnowledgeBoundary();
        boundary.setProvidedFacts(providedFacts(content));
        boundary.setInferences(List.of(
                inference("该想法的核心风险集中在 " + profile.title() + " 的真实使用动机和验证路径。", "来自用户输入主题和 Mock 分类结果。", "MEDIUM"),
                inference("如果输入没有提供商业模式，付费意愿应视为待验证而不是事实。", "系统只检测到有限的商业线索。", "MEDIUM")
        ));
        boundary.setCriticalHypotheses(List.of(
                hypothesis("目标用户愿意在当前替代方案之外使用新产品。", "HIGH", "如果不成立，产品难以形成持续使用。", List.of("用户痛点", "增长潜力")),
                hypothesis("第一版收缩后的核心闭环足以验证价值。", "HIGH", "如果不成立，继续开发会变成堆功能。", List.of("实现难度", "风险等级")),
                hypothesis("存在明确买单方或可持续获客方式。", "MEDIUM", "如果不成立，商业模式和增长路径都需要重写。", List.of("商业模式", "市场空间"))
        ));
        boundary.setUnknowns(List.of(
                unknown("谁会为这个产品持续付费或投入迁移成本？", "输入中尚未提供可验证的付费意愿或采购流程。"),
                unknown("用户当前最不满意的替代方案是什么？", "缺少对现有方案的具体比较。"),
                unknown("首批用户从哪里来？", "冷启动渠道仍需要验证。")
        ));
        return boundary;
    }

    private List<ReviewReportDto.ProvidedFact> providedFacts(String content) {
        List<ReviewReportDto.ProvidedFact> facts = new ArrayList<>();
        addFactIfContains(facts, content, "目标用户", "用户输入提到了目标用户。");
        addFactIfContains(facts, content, "场景", "用户输入提到了使用场景。");
        addFactIfContains(facts, content, "痛点", "用户输入提到了要解决的痛点。");
        addFactIfContains(facts, content, "商业", "用户输入提到了商业模式相关线索。");
        if (facts.isEmpty() && content.length() > 0) {
            ReviewReportDto.ProvidedFact fact = new ReviewReportDto.ProvidedFact();
            fact.setStatement("用户提交了产品想法文本。");
            fact.setSourceText(content.length() <= 80 ? content : content.substring(0, 80) + "...");
            facts.add(fact);
        }
        return facts;
    }

    private void addFactIfContains(List<ReviewReportDto.ProvidedFact> facts, String content, String keyword, String statement) {
        if (!content.contains(keyword)) {
            return;
        }
        ReviewReportDto.ProvidedFact fact = new ReviewReportDto.ProvidedFact();
        fact.setStatement(statement);
        fact.setSourceText(excerpt(content, keyword));
        facts.add(fact);
    }

    private ReviewReportDto.Inference inference(String statement, String basis, String confidence) {
        ReviewReportDto.Inference item = new ReviewReportDto.Inference();
        item.setStatement(statement);
        item.setBasis(basis);
        item.setConfidence(confidence);
        return item;
    }

    private ReviewReportDto.CriticalHypothesis hypothesis(String statement, String importance, String impact, List<String> dimensions) {
        ReviewReportDto.CriticalHypothesis item = new ReviewReportDto.CriticalHypothesis();
        item.setStatement(statement);
        item.setImportance(importance);
        item.setImpactIfFalse(impact);
        item.setRelatedDimensions(dimensions);
        return item;
    }

    private ReviewReportDto.UnknownQuestion unknown(String question, String reason) {
        ReviewReportDto.UnknownQuestion item = new ReviewReportDto.UnknownQuestion();
        item.setQuestion(question);
        item.setReason(reason);
        return item;
    }

    private ReviewReportDto.OverallAssessment overallAssessment(ReviewReportDto report, IdeaProfile profile) {
        ReviewReportDto.OverallAssessment overall = new ReviewReportDto.OverallAssessment();
        int overallScore = score(Math.round(report.getPositioningScore() * 0.7f + (100 - report.getBeatScore()) * 0.3f));
        overall.setOverallScore(overallScore);
        overall.setDecision(switch (profile.goDecision()) {
            case "CONTINUE" -> "PROCEED";
            case "PIVOT" -> "NARROW_SCOPE";
            default -> "VALIDATE_FIRST";
        });
        overall.setStrongestDimension("用户痛点");
        overall.setWeakestDimension(report.getPositioningScore() < 55 ? "产品定位" : "商业模式");
        overall.setMostUncertainDimension("商业模式");
        overall.setPriorityQuestion("谁会为了这个问题付费、迁移或持续使用？");
        overall.setOverallReasoning("Mock 判断仅用于本地演示：当前输入能支撑初步评审，但关键商业和冷启动假设仍需验证。");
        return overall;
    }

    private ReviewReportDto.QualityAudit initialQualityAudit() {
        ReviewReportDto.QualityAudit audit = new ReviewReportDto.QualityAudit();
        audit.setOverallStatus("WARNING");
        audit.setSummary("Mock 已生成可信评审结构，最终状态由后端规则检查重新聚合。");
        return audit;
    }

    private String confidence(boolean high, boolean medium) {
        if (high) {
            return "HIGH";
        }
        return medium ? "MEDIUM" : "LOW";
    }

    private List<String> evidence(String content, String... values) {
        List<String> result = new ArrayList<>(List.of(values));
        if (content.length() > 0) {
            result.add("输入摘要：" + (content.length() <= 60 ? content : content.substring(0, 60) + "..."));
        }
        return result;
    }

    private String excerpt(String content, String keyword) {
        int index = content.indexOf(keyword);
        if (index < 0) {
            return content.length() <= 80 ? content : content.substring(0, 80) + "...";
        }
        int start = Math.max(0, index - 20);
        int end = Math.min(content.length(), index + keyword.length() + 40);
        return content.substring(start, end);
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
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
            int baseClarity,
            String content) {

        static IdeaProfile from(String content) {
            String lower = content.toLowerCase(Locale.ROOT);
            if (contains(content, "浏览器插件", "WebSocket", "Electron", "React", "桌宠", "通知", "系统托盘")
                    || lower.contains("electron")
                    || lower.contains("react")
                    || lower.contains("websocket")) {
                return new IdeaProfile(title(content, "桌面通知工具"), "desktop_tool", "NEW_IDEA", "PIVOT", 64, 58, content);
            }
            if (contains(content, "甲方", "客户", "外包", "报价", "验收", "需求文档")) {
                return new IdeaProfile(title(content, "甲方需求"), "client_requirement", "CLIENT_REQUIREMENT", "PIVOT", 72, 48, content);
            }
            if (contains(content, "飞书", "钉钉", "企业微信", "Notion", "Slack", "微信读书")) {
                return new IdeaProfile(title(content, "成熟产品复盘"), "mature_product", "MATURE_PRODUCT", "PIVOT", 58, 56, content);
            }
            if (isCampusTrade(content)) {
                return new IdeaProfile(title(content, "校园二手交易"), "campus_trade", "NEW_IDEA", "CONTINUE", 54, 68, content);
            }
            if (contains(content, "简历", "求职", "岗位", "面试", "招聘") || lower.contains("resume") || lower.contains("jd")) {
                return new IdeaProfile(title(content, "AI 简历优化"), "ai_resume", "NEW_IDEA", "PIVOT", 66, 62, content);
            }
            if (contains(content, "宠物", "猫", "狗", "疫苗", "驱虫", "健康", "兽医")) {
                return new IdeaProfile(title(content, "宠物健康管理"), "pet_health", "NEW_IDEA", "CONTINUE", 61, 59, content);
            }
            if (content.length() >= 18 && contains(content, "App", "APP", "小程序", "工具", "平台", "系统", "助手")) {
                return new IdeaProfile(title(content, "新产品想法"), "generic", "NEW_IDEA", genericDecision(content), genericRisk(content), genericClarity(content), content);
            }
            return new IdeaProfile(title(content, "未明确输入"), "unclear", "UNCLEAR", "PAUSE", 86, 22, content);
        }

        private boolean hasTargetUser() {
            return contains(content, "目标用户", "用户是", "面向", "给", "店长", "老板", "学生", "求职者", "主人", "甲方", "客户");
        }

        private boolean hasScene() {
            return contains(content, "使用场景", "场景", "每天", "闭店", "毕业季", "投递", "就医", "通知", "验收");
        }

        private boolean hasBusinessModel() {
            return contains(content, "商业模式", "付费", "收费", "订阅", "采购", "广告", "收入", "抽佣");
        }

        private boolean hasDifferentiation() {
            return contains(content, "不同", "差异", "替代", "现有", "相比", "只聚焦", "不够");
        }

        private boolean hasValidation() {
            return contains(content, "验证", "测试", "访谈", "数据", "指标", "试点", "首批");
        }

        private List<String> missingInputParts() {
            List<String> missing = new ArrayList<>();
            if (!hasTargetUser()) {
                missing.add("目标用户");
            }
            if (!hasScene()) {
                missing.add("使用场景");
            }
            if (!hasDifferentiation()) {
                missing.add("差异化");
            }
            if (!hasBusinessModel()) {
                missing.add("商业模式");
            }
            if (!hasValidation()) {
                missing.add("验证路径");
            }
            return missing;
        }

        private static String genericDecision(String content) {
            return genericCompleteness(content) >= 4 ? "PIVOT" : "PAUSE";
        }

        private static int genericRisk(String content) {
            return Math.max(58, 86 - genericCompleteness(content) * 6);
        }

        private static int genericClarity(String content) {
            return Math.min(68, 32 + genericCompleteness(content) * 8);
        }

        private static int genericCompleteness(String content) {
            int value = 0;
            if (contains(content, "目标用户", "用户是", "面向", "给", "店长", "老板", "学生", "求职者", "主人", "甲方", "客户")) {
                value++;
            }
            if (contains(content, "使用场景", "场景", "每天", "闭店", "毕业季", "投递", "就医", "通知", "验收")) {
                value++;
            }
            if (contains(content, "痛点", "问题", "断货", "压货", "不够", "成本")) {
                value++;
            }
            if (contains(content, "商业模式", "付费", "收费", "订阅", "采购", "广告", "收入", "抽佣")) {
                value++;
            }
            if (contains(content, "不同", "差异", "替代", "现有", "相比", "只聚焦")) {
                value++;
            }
            if (contains(content, "验证", "测试", "访谈", "数据", "指标", "试点", "首批")) {
                value++;
            }
            return value;
        }

        private static boolean isCampusTrade(String content) {
            boolean campusContext = contains(content, "校园", "校区", "同校", "学生", "教材", "宿舍", "毕业季");
            boolean tradeContext = contains(content, "二手", "闲置", "转卖", "交易", "卖家", "买家");
            return campusContext && tradeContext;
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
