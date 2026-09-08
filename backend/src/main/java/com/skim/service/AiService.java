package com.skim.service;

import com.skim.util.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int MAX_PROMPT_LENGTH = 20_000;

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String callAi(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new AiServiceException("Prompt must not be empty", null);
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new AiServiceException(
                    "Prompt exceeds maximum allowed length of " + MAX_PROMPT_LENGTH + " characters", null);
        }

        try {
            String response = chatClient.prompt().user(prompt).call().content();
            if (response == null || response.isBlank()) {
                throw new AiServiceException("AI returned an empty response", null);
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI call failed. Prompt length: {}", prompt.length(), e);
            throw new AiServiceException("Failed to generate AI response", e);
        }
    }
}