package com.skim.service;

import com.skim.util.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);


    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String askGemini(String data) {

        try {
            return chatClient.prompt().user(data).call().content();
        } catch (Exception e) {
            log.error("AI call failed for askGemini", e);
            throw new RuntimeException("Failed to get AI response", e);
        }
    }

    public String callAi(String prompt) {
        try {
            String response = chatClient.prompt().user(prompt).call().content();
            if (response == null || response.isBlank()) {
                throw new AiServiceException("AI returned empty response for ", null);
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI call failed while generating {}", e);
            throw new AiServiceException("Failed to generate " , e);
        }
    }
}
