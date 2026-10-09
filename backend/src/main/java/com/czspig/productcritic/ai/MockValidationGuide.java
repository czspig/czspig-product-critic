package com.czspig.productcritic.ai;

import com.czspig.productcritic.dto.ReviewReportDto;
import java.util.List;

/** Synthetic proposals only: no interview, payment or market result is asserted. */
final class MockValidationGuide {
    static ReviewReportDto.ValidationGuide build(ReviewReportDto report, String input) {
        String excerpt = input.length() > 160 ? input.substring(0, 160) + "…" : input;
        var demand = new ReviewReportDto.RiskExperiment("目标用户的痛点是否值得换方案", "先确认问题成立，避免把功能清单当需求", "HYPOTHESIS",
                "用户提交方案：" + excerpt + "；尚未核验真实行为", "若用户现有办法已经够用，开发再多功能也无法创造迁移动机", "输入中描述的目标用户；未写明时先选定一种人群",
                List.of("第 1–2 天招募 3–5 位符合场景的用户，询问最近一次真实任务", "请对方展示当前办法，记录卡点与代价，避免只问喜不喜欢"),
                List.of("原始任务记录、当前替代方案、用户原话和发生频次"), "参考：至少 3 人能展示近期相同卡点，否则先调整人群或问题",
                "3–5 人用于发现重复问题，不能推算市场规模；3 人是防止单个偶然案例主导判断的探索阈值");
        var value = new ReviewReportDto.RiskExperiment("最小方案能否改善核心任务", "确认痛点后再检验解法，而不是继续堆功能", "HYPOTHESIS", "当前提供的是方案描述，尚无任务试用记录",
                "如果不能比现有办法更省时或更可靠，描述完善仍不等于风险降低", "有上述痛点的用户",
                List.of("第 3–5 天用纸面原型、表格或人工代办完成一条核心流程", "让同一用户用现有办法和原型处理同类任务，记录帮助次数和卡点"),
                List.of("任务完成情况、时间、错误、人工帮助次数和再次使用行为"), "参考：多数测试者无需关键帮助完成任务，并能说出相对现有办法的具体收益",
                "优先观察实际完成和收益，避免用口头好评替代价值；小样本不代表留存已经成立");
        var viability = new ReviewReportDto.RiskExperiment("持续投入与交付成本是否可承担", "价值存在后还要确认资源和责任，不应默认有人付费", "HYPOTHESIS", "目前未核验预算、付费或长期运营记录",
                "即使有人试用，成本或责任缺口仍可能让项目无法持续", "买单人、验收人或实际维护者",
                List.of("第 6–7 天向责任人展示任务结果与成本清单", "确认下一次试用的时间、数据责任和预算条件；拒绝时记录理由"),
                List.of("明确的下一步承诺、单次交付成本、预算条件及拒绝原因"), "参考：至少一位责任人确认可执行下一步，且成本低于团队事先约定的预算上限",
                "明确承诺用于区别泛泛支持；不是付费意愿已验证，预算上限须由团队先定");
        return new ReviewReportDto.ValidationGuide(true, "Mock 演示：输入描述了方案，但尚无核验后的用户行为与成本证据，先验证再投入。",
                List.of(demand, value, viability), "痛点重复出现、任务收益可观察且责任人与预算明确：继续小范围验证。",
                "痛点存在但原型收益不足或责任对象不同：调整场景、解法或范围后重测。",
                "找不到重复痛点、持续试验仍无任务收益或成本越过事先预算：暂缓扩展；信息不足则补证据，不直接判死刑。");
    }
}
