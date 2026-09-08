package com.skim.service;

import com.skim.dto.SkimRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class SkimService {

    private final AiService aiService;
    public String processContent(SkimRequest request) {
        String prompt = buildPrompt(request);
        String aiCall = aiService.callAi(prompt);
        System.out.println("yooo");
        System.out.println(aiCall);
        return aiCall;
    }

    private String buildPrompt(SkimRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Content must not be empty");
        }

        String operation = request.getOperation();
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Operation must not be empty");
        }

        StringBuilder prompt = new StringBuilder();

        switch (operation.toLowerCase()) {
            case "summarize":
                prompt.append(
                        "Provide a clear and concise summary of the following text in a few sentences:\n\n"
                );
                break;

            case "suggest":
                prompt.append(
                        "Based on the following content, suggest related topics and further reading. "
                                + "Format the response with clear headings and bullet points:\n\n"
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        return prompt.append(request.getContent()).toString();
    }
}
