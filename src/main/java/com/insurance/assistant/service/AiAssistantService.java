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

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final InsuranceTools insuranceTools;

    public AiAssistantService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                               InsuranceTools insuranceTools) {
        // No defaultSystem() here - the system prompt varies per question
        // type (policy / claim / out-of-scope) and is passed in per call.
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.insuranceTools = insuranceTools;
    }

    public AiChatResponse answer(String question) {
        try {
            // Questions that look unrelated to insurance skip vector search
            // and tools entirely, and just get a short, polite redirect.
            if (isOutOfScope(question)) {
                return answerOutOfScope(question);
            }

            List<Document> relevantChunks = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(TOP_K).build());

            // Phase 7: even with no matching document chunks, the question
            // might still be answerable via a tool (e.g. "what's the status
            // of claim CLM-2026-4821?" won't match any policy document, but
            // the model can answer it directly using getClaimStatus).
            String context = relevantChunks.isEmpty()
                    ? "(No matching policy document excerpts were found for this question.)"
                    : relevantChunks.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

            String systemPrompt = isClaimRelated(question)
                    ? PromptTemplates.CLAIM_SYSTEM_PROMPT
                    : PromptTemplates.SYSTEM_PROMPT;

            String userPrompt = PromptTemplates.buildUserPrompt(context, question);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(insuranceTools)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("AI provider returned an empty response");
                return new AiChatResponse(FALLBACK_ANSWER, List.of());
            }

            // Document sources only make sense when the answer actually drew
            // on document chunks - a tool-based answer (claim lookup, policy
            // lookup) has no document source to cite.
            List<String> sources = relevantChunks.stream()
                    .map(chunk -> (String) chunk.getMetadata().get("source"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            return new AiChatResponse(response, sources);
        } catch (Exception ex) {
            // Covers a missing/invalid API key, the provider being down,
            // timeouts, rate limits, vector search failures, and tool-call
            // failures - we don't distinguish these to the caller, but we
            // log enough to diagnose it ourselves. Never log the question's
            // full content, since it could contain sensitive information
            // about the user's situation.
            log.error("AI request failed: {}", ex.getMessage());
            return new AiChatResponse(FALLBACK_ANSWER, List.of());
        }
    }

    /**
     * Short-circuit path for questions that aren't about insurance at all -
     * no document context or tools are needed, just a polite redirect.
     */
    private AiChatResponse answerOutOfScope(String question) {
        try {
            String userPrompt = PromptTemplates.buildOutOfScopeUserPrompt(question);

            String response = chatClient.prompt()
                    .system(PromptTemplates.OUT_OF_SCOPE_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return new AiChatResponse(FALLBACK_ANSWER, List.of());
            }

            return new AiChatResponse(response, List.of());
        } catch (Exception ex) {
            log.error("AI request failed: {}", ex.getMessage());
            return new AiChatResponse(FALLBACK_ANSWER, List.of());
        }
    }

    /**
     * Simple keyword check for claim-related questions, so they get the
     * more empathetic claim system prompt instead of the general one.
     */
    private boolean isClaimRelated(String question) {
        String q = question.toLowerCase();
        return q.contains("claim") || q.contains("reimbursement")
                || q.contains("settlement") || q.contains("file a claim");
    }

    /**
     * Simple keyword check for questions clearly unrelated to insurance.
     * Deliberately conservative - only skips RAG/tools for obvious cases, so
     * an ambiguous question still goes through the normal grounded path.
     */
    private boolean isOutOfScope(String question) {
        String q = question.toLowerCase();
        return q.matches(".*\\b(weather|cricket|movie|recipe|football|politics)\\b.*");
    }
}