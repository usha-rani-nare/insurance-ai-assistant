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

    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        aiAssistantService = new AiAssistantService(chatClientBuilder, vectorStore);
    }

    private Document sampleChunk(String source) {
        return new Document("Hospitalization is covered up to the plan's limit.", Map.of("source", source));
    }

    @Test
    void returnsAnAnswerWithSourcesWhenRelevantChunksAreFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(sampleChunk("hospitalization-coverage.txt")));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Yes, hospitalization is covered.");

        AiChatResponse response = aiAssistantService.answer("Does my policy cover hospitalization?");

        assertThat(response.getAnswer()).isEqualTo("Yes, hospitalization is covered.");
        assertThat(response.getSources()).containsExactly("hospitalization-coverage.txt");
    }

    @Test
    void returnsNoContextMessageWhenNothingRelevantIsFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        AiChatResponse response = aiAssistantService.answer("Does this cover international space travel?");

        assertThat(response.getAnswer()).isEqualTo(AiAssistantService.NO_CONTEXT_ANSWER);
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
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("   ");

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
}
