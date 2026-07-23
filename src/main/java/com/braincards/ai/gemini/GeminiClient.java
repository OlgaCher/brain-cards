package com.braincards.ai.gemini;

import com.braincards.ai.AiCoachException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${braincards.ai.gemini.base-url}") String baseUrl,
                        @Value("${braincards.ai.gemini.api-key:}") String apiKey,
                        @Value("${braincards.ai.gemini.model}") String model) {
        this.restClient = RestClient.create(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiCoachException("AI help is not configured yet (missing GEMINI_API_KEY).");
        }

        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));

        GeminiResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Gemini call failed: {} model={} body={}",
                    e.getStatusCode(), model, e.getResponseBodyAsString());
            throw new AiCoachException("AI service error " + e.getStatusCode().value()
                    + " (model '" + model + "'). See server logs for details.", e);
        } catch (RestClientException e) {
            log.error("Gemini call failed (no response) model={}", model, e);
            throw new AiCoachException("The AI service is temporarily unavailable. Please try again.", e);
        }

        return firstText(response);
    }

    private String firstText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiCoachException("The AI service returned an empty response.");
        }
        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new AiCoachException("The AI service returned an empty response.");
        }
        String text = candidate.content().parts().get(0).text();
        if (text == null || text.isBlank()) {
            throw new AiCoachException("The AI service returned an empty response.");
        }
        return text.trim();
    }
}
