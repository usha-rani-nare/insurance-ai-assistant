package com.insurance.assistant.service;

import com.insurance.assistant.dto.AiChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private VectorStore vectorStore;

    // Phase 7: AiAssistantService now also depends on InsuranceTools, so the
    // chain includes a .tools(insuranceTools) call that needs stubbing too.
    @Mock
    private InsuranceTools insuranceTools;

    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        aiAssistantService = new AiAssistantService(chatClientBuilder, vectorStore, insuranceTools);
    }

    private Document sampleChunk(String source) {
        return new Document("Hospitalization is covered up to the plan's limit.", Map.of("source", source));
    }

    /**
     * Stubs the full chatClient.prompt().system(...).user(...).tools(...).call().content()
     * chain used by every non-out-of-scope path in the service.
     */
    private void stubChatClientToReturn(String content) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(content);
    }

    @Test
    void returnsAnAnswerWithSourcesWhenRelevantChunksAreFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(sampleChunk("hospitalization-coverage.txt")));
        stubChatClientToReturn("Yes, hospitalization is covered.");

        AiChatResponse response = aiAssistantService.answer("Does my policy cover hospitalization?");

        assertThat(response.getAnswer()).isEqualTo("Yes, hospitalization is covered.");
        assertThat(response.getSources()).containsExactly("hospitalization-coverage.txt");
    }

    @Test
    void returnsAnAnswerWithNoSourcesWhenNoChunksMatchButAToolCanStillAnswer() {
        // Phase 7: e.g. "what's the status of claim CLM-2026-4821?" won't
        // match any policy document, but the model can still answer via the
        // getClaimStatus tool - so this should NOT fall back to NO_CONTEXT_ANSWER.
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        stubChatClientToReturn("Your claim CLM-2026-4821 is currently under review.");

        AiChatResponse response = aiAssistantService.answer("What's the status of claim CLM-2026-4821?");

        assertThat(response.getAnswer()).isEqualTo("Your claim CLM-2026-4821 is currently under review.");
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void returnsFallbackMessageWhenTheProviderThrows() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(sampleChunk("hospitalization-coverage.txt")));
        when(chatClient.prompt()).thenThrow(new RuntimeException("provider unavailable"));

        AiChatResponse response = aiAssistantService.answer("Does my policy cover hospitalization?");

        assertThat(response.getAnswer()).isEqualTo(AiAssistantService.FALLBACK_ANSWER);
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void returnsFallbackMessageWhenTheProviderReturnsAnEmptyResponse() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(sampleChunk("hospitalization-coverage.txt")));
        stubChatClientToReturn("   ");

        AiChatResponse response = aiAssistantService.answer("Does my policy cover hospitalization?");

        assertThat(response.getAnswer()).isEqualTo(AiAssistantService.FALLBACK_ANSWER);
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void returnsFallbackMessageWhenVectorSearchThrows() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        AiChatResponse response = aiAssistantService.answer("Does my policy cover hospitalization?");

        assertThat(response.getAnswer()).isEqualTo(AiAssistantService.FALLBACK_ANSWER);
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void usesClaimPromptPathWhenQuestionMentionsClaim() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(sampleChunk("claims-process.txt")));
        stubChatClientToReturn("Your claim is currently under review.");

        AiChatResponse response = aiAssistantService.answer("What's the status of my claim?");

        assertThat(response.getAnswer()).isEqualTo("Your claim is currently under review.");
        assertThat(response.getSources()).containsExactly("claims-process.txt");
    }

    @Test
    void skipsVectorSearchAndRedirectsWhenQuestionIsOutOfScope() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn("I can only help with insurance-related questions - happy to help with those!");

        AiChatResponse response = aiAssistantService.answer("What's the weather today?");

        assertThat(response.getAnswer())
                .isEqualTo("I can only help with insurance-related questions - happy to help with those!");
        assertThat(response.getSources()).isEmpty();
    }
}