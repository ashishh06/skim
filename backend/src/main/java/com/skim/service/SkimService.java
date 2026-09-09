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
        return aiService.callAi(prompt);
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

            case "explain":
                prompt.append(
                        "Explain the following text in simple, easy to understand terms, as if explaining "
                                + "it to someone with no background in the subject. Avoid jargon:\n\n"
                );
                break;

            case "rewrite":
                prompt.append(buildRewriteInstruction(request.getTone()));
                break;

            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        return prompt.append(request.getContent()).toString();
    }

    private String buildRewriteInstruction(String tone) {
        if (tone == null || tone.isBlank()) {
            throw new IllegalArgumentException("Tone must not be empty for rewrite operation");
        }

        switch (tone.toLowerCase()) {
            case "formal":
                return "Rewrite the following text in a formal, professional tone:\n\n";

            case "casual":
                return "Rewrite the following text in a casual, conversational tone:\n\n";

            case "fix-grammar":
                return "Fix any grammar, spelling, and punctuation mistakes in the following text. "
                        + "Keep the original meaning and tone intact, and only correct actual errors:\n\n";

            case "shorten":
                return "Rewrite the following text to be significantly shorter while keeping the key meaning:\n\n";

            case "expand":
                return "Expand the following text with more detail and explanation while keeping the "
                        + "original meaning intact:\n\n";

            default:
                throw new IllegalArgumentException("Unknown tone: " + tone);
        }
    }
}