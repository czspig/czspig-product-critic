package com.czspig.productcritic.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.czspig.productcritic.config.AppAiProperties;
import com.czspig.productcritic.config.DeepSeekProperties;
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
    void shouldFallbackToMockWhenDeepSeekHasNoApiKey() {
        AppAiProperties app = new AppAiProperties();
        app.setProvider("deepseek");
        app.setFallbackToMock(true);
        DeepSeekProperties deepSeek = new DeepSeekProperties();

        AiProviderRouter router = new AiProviderRouter(app, deepSeek, mockProvider, deepSeekProvider);

        assertThat(router.providerName()).isEqualTo("mock");
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
}
