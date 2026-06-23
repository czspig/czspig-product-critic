package com.czspig.productcritic.ai;

import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.common.ErrorCode;
import com.czspig.productcritic.config.AppAiProperties;
import com.czspig.productcritic.config.DeepSeekProperties;
import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class AiProviderRouter implements AiProvider {

    private final AppAiProperties appAiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final MockAiProvider mockAiProvider;
    private final DeepSeekAiProvider deepSeekAiProvider;
    private final ThreadLocal<AiProvider> lastProvider = new ThreadLocal<>();

    public AiProviderRouter(
            AppAiProperties appAiProperties,
            DeepSeekProperties deepSeekProperties,
            MockAiProvider mockAiProvider,
            DeepSeekAiProvider deepSeekAiProvider) {
        this.appAiProperties = appAiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.mockAiProvider = mockAiProvider;
        this.deepSeekAiProvider = deepSeekAiProvider;
    }

    @Override
    public ReviewReportDto review(CreateReviewRequest request, String prompt) {
        AiProvider selected = selectProvider();
        lastProvider.set(selected);
        try {
            ReviewReportDto report = selected.review(request, prompt);
            lastProvider.set(selected);
            return report;
        } catch (RuntimeException ex) {
            if (selected != mockAiProvider && appAiProperties.isFallbackToMock()) {
                ReviewReportDto fallbackReport = mockAiProvider.review(request, prompt);
                lastProvider.set(mockAiProvider);
                return fallbackReport;
            }
            if (ex instanceof BizException) {
                throw ex;
            }
            throw new BizException(ErrorCode.AI_PROVIDER_ERROR, "AI 评审服务暂时不可用，请稍后再试");
        }
    }

    @Override
    public String providerName() {
        AiProvider provider = lastProvider.get();
        return provider == null ? selectProvider().providerName() : provider.providerName();
    }

    @Override
    public String modelName() {
        AiProvider provider = lastProvider.get();
        return provider == null ? selectProvider().modelName() : provider.modelName();
    }

    private AiProvider selectProvider() {
        String provider = appAiProperties.getProvider() == null ? "auto" : appAiProperties.getProvider().trim();
        if ("mock".equalsIgnoreCase(provider)) {
            return mockAiProvider;
        }
        if ("deepseek".equalsIgnoreCase(provider)) {
            if (!deepSeekProperties.hasApiKey() && appAiProperties.isFallbackToMock()) {
                return mockAiProvider;
            }
            return deepSeekAiProvider;
        }
        return deepSeekProperties.hasApiKey() ? deepSeekAiProvider : mockAiProvider;
    }
}
