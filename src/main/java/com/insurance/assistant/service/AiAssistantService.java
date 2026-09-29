package com.insurance.assistant.service;

import com.insurance.assistant.dto.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    // Package-visible so the test can assert against the exact same message
    // rather than duplicating the string.
    static final String FALLBACK_ANSWER = "The AI assistant is temporarily unavailable. Please try again.";
    static final String NO_CONTEXT_ANSWER =
            "I couldn't find anything about that in the available policy documents.";

    private static final int TOP_K = 3;

    // Kept here rather than in the controller, so the controller only deals
    // with HTTP concerns and this is the one place to tune the assistant's
    // behavior.
    private static final String SYSTEM_PROMPT = """
            You are an insurance assistant. You will be given policy
            document excerpts as context along with the customer's
            question. Answer using only that context.
            Do not invent policy details, coverage limits, or claim
            information that isn't in the context you were given.
            If the context doesn't contain enough information to answer
            accurately, say so plainly instead of guessing.
            Do not tell a customer they are eligible for coverage unless
            the context actually supports it.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Policy document excerpts:
            ---
            %s
            ---

            Question: %s
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AiAssistantService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.vectorStore = vectorStore;
    }

    public AiChatResponse answer(String question) {
        try {
            List<Document> relevantChunks = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(TOP_K).build());

            if (relevantChunks.isEmpty()) {
                log.warn("No relevant policy document chunks found for the question");
                return new AiChatResponse(NO_CONTEXT_ANSWER, List.of());
            }

            String context = relevantChunks.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            String userPrompt = USER_PROMPT_TEMPLATE.formatted(context, question);

            String response = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("AI provider returned an empty response");
                return new AiChatResponse(FALLBACK_ANSWER, List.of());
            }

            List<String> sources = relevantChunks.stream()
                    .map(chunk -> (String) chunk.getMetadata().get("source"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            return new AiChatResponse(response, sources);
        } catch (Exception ex) {
            // Covers a missing/invalid API key, the provider being down,
            // timeouts, rate limits, and vector search failures - we don't
            // distinguish these to the caller, but we log enough to
            // diagnose it ourselves. Never log the question's full content,
            // since it could contain sensitive information about the user's
            // situation.
            log.error("AI request failed: {}", ex.getMessage());
            return new AiChatResponse(FALLBACK_ANSWER, List.of());
        }
    }
}
