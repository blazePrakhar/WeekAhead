package com.weekahead.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.weekahead.ai.exception.AIProviderException;

@Component
@ConditionalOnProperty(
        name = "ai.provider",
        havingValue = "gemini",
        matchIfMissing = true
)
public class GeminiLLMClient implements LLMClient {

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiLLMClient(
            ObjectMapper objectMapper,
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-3.1-flash-lite}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generateInsight(String structuredSummary) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException(
                    "AI provider is not configured"
            );
        }

        try (Client client = Client.builder()
                .apiKey(apiKey)
                .build()) {

            String prompt = """
                    You are the weekly planning assistant for WeekAhead.

                    Use ONLY the backend-calculated facts provided below.

                    Your job is to:
                    1. Summarize the user's week.
                    2. Identify important observations.
                    3. Suggest practical actions for the coming week.

                    Rules:
                    - Do not invent facts.
                    - Do not recalculate allocations.
                    - Do not change recommendations.
                    - Do not create new time allocations.
                    - Do not access databases.
                    - Do not claim access to user data beyond the supplied summary.
                    - Keep the response concise and practical.
                    - Return ONLY the requested JSON structure.

                    Backend-calculated facts:

                    %s
                    """.formatted(structuredSummary);

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .responseMimeType("application/json")
                            .responseSchema(buildResponseSchema())
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            model,
                            prompt,
                            config
                    );

            String text = response.text();

            if (text == null || text.isBlank()) {
                throw new AIProviderException(
                        "AI provider returned an empty response"
                );
            }

            // Validate that Gemini returned valid JSON before
            // handing it back to AIInsightService.
            objectMapper.readTree(text);

            return text;

        } catch (AIProviderException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new AIProviderException(
                    "AI provider request failed",
                    exception
            );
        }
    }

    private Schema buildResponseSchema() {

        Schema stringListSchema =
                Schema.builder()
                        .type(Type.Known.ARRAY)
                        .items(
                                Schema.builder()
                                        .type(Type.Known.STRING)
                                        .build()
                        )
                        .build();

        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(
                        Map.of(
                                "summary",
                                Schema.builder()
                                        .type(Type.Known.STRING)
                                        .build(),

                                "observations",
                                stringListSchema,

                                "actions",
                                stringListSchema
                        )
                )
                .required(
                        List.of(
                                "summary",
                                "observations",
                                "actions"
                        )
                )
                .build();
    }
}