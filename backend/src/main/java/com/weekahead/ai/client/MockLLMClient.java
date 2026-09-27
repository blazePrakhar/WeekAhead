package com.weekahead.ai.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "ai.provider",
        havingValue = "mock"
)
public class MockLLMClient implements LLMClient {

    @Override
    public String generateInsight(String structuredSummary) {

        return """
                {
                  "summary": "Your week was reviewed using the backend-provided data.",
                  "observations": [
                    "The weekly planning data was processed successfully."
                  ],
                  "actions": [
                    "Continue reviewing your weekly allocation and actual time."
                  ]
                }
                """;
    }
}