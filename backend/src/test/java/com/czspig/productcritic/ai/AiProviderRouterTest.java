package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.czspig.productcritic.common.BizException;
import com.czspig.productcritic.config.AppAiProperties;
import com.czspig.productcritic.config.DeepSeekProperties;
import com.czspig.productcritic.dto.CreateReviewRequest;
import org.junit.jupiter.api.Test;

class AiProviderRouterTest {

    private final MockAiProvider mockProvider = new MockAiProvider();
    private final DeepSeekAiProvider deepSeekProvider = mock(DeepSeekAiProvider.class);

    @Test
    void shouldUseMockWhenProviderIsMock() {
        AppAiProperties app = new AppAiProperties();
        app.setProvider("mock");
        DeepSeekProperties deepSeek = new DeepSeekProperties();
        when(deepSeekProvider.providerName()).thenReturn("deepseek");

        AiProviderRouter router = new AiProviderRouter(app, deepSeek, mockProvider, deepSeekProvider);

        assertThat(router.providerName()).isEqualTo("mock");
        assertThat(router.modelName()).isEqualTo("mock-product-reviewer-v2");
    }

    @Test
    void shouldFallbackToMockWhenDeepSeekHasNoApiKeyAndFallbackIsExplicitlyEnabled() {
        AppAiProperties app = new AppAiProperties();
        app.setProvider("deepseek");
        app.setFallbackToMock(true);
        DeepSeekProperties deepSeek = new DeepSeekProperties();

        AiProviderRouter router = new AiProviderRouter(app, deepSeek, mockProvider, deepSeekProvider);

        assertThat(router.providerName()).isEqualTo("mock");
    }

    @Test
    void shouldFailDeepSeekWithoutApiKeyWhenFallbackIsDisabled() {
        AppAiProperties app = new AppAiProperties();
        app.setProvider("deepseek");
        app.setFallbackToMock(false);
        DeepSeekProperties deepSeek = new DeepSeekProperties();

        AiProviderRouter router = new AiProviderRouter(app, deepSeek, mockProvider, deepSeekProvider);

        assertThatThrownBy(() -> router.review(request(), "prompt"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置 DeepSeek API Key");
    }

    @Test
    void shouldUseDeepSeekInAutoModeWhenApiKeyExists() {
        AppAiProperties app = new AppAiProperties();
        app.setProvider("auto");
        DeepSeekProperties deepSeek = new DeepSeekProperties();
        deepSeek.setApiKey("test-key");
        when(deepSeekProvider.providerName()).thenReturn("deepseek");
        when(deepSeekProvider.modelName()).thenReturn("deepseek-test");

        AiProviderRouter router = new AiProviderRouter(app, deepSeek, mockProvider, deepSeekProvider);

        assertThat(router.providerName()).isEqualTo("deepseek");
        assertThat(router.modelName()).isEqualTo("deepseek-test");
    }

    private CreateReviewRequest request() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setContent("测试产品想法，包含目标用户和使用场景");
        request.setMode("SHARP_PM");
        request.setRoastLevel(2);
        return request;
    }
}
