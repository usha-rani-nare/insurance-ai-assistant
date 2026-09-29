package com.insurance.assistant.dto;

import java.util.List;

public class AiChatResponse {

    private final String answer;
    private final List<String> sources;

    public AiChatResponse(String answer, List<String> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getSources() {
        return sources;
    }
}
