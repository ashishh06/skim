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
        return aiCall;
    }

    private String buildPrompt(SkimRequest request){
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperation()) {
            case "summarize":
                prompt.append("Provide a clear and concise summary of the following text in a few sentences:\\n\\n");
            case "suggest":
                prompt.append("Based on the following content: suggest related topics and further reading. Format the response with clear headings and bullet points:\n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown Operation: " + request.getOperation());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}
