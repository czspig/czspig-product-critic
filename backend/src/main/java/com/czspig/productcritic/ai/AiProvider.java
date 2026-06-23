package com.czspig.productcritic.ai;

import com.czspig.productcritic.dto.CreateReviewRequest;
import com.czspig.productcritic.dto.ReviewReportDto;

public interface AiProvider {

    ReviewReportDto review(CreateReviewRequest request, String prompt);

    String providerName();

    String modelName();
}
