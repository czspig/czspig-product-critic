package com.czspig.productcritic.ai;

import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;

public interface AiProvider {

    record ProviderExecution(String providerName, String modelName, boolean fallbackUsed) {
    }

    ReviewReportDto review(CreateReviewRequest request, String prompt);

    String providerName();

    String modelName();

    default ProviderExecution execution() {
        return new ProviderExecution(providerName(), modelName(), false);
    }
}
