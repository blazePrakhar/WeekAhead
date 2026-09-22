package com.weekahead.ai.client;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.weekahead.ai.dto.AIInsightContent;

@Component
public class OpenAILLMClient implements LLMClient {

    private final ObjectMapper objectMapper;

    public OpenAILLMClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateInsight(String structuredSummary) {
        try {
            OpenAIClient client = OpenAIOkHttpClient.fromEnv();

            StructuredResponseCreateParams<AIInsightContent> params =
                    ResponseCreateParams.builder()
                            .input("""
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
                                    - Do not claim access to databases or user data.
                                    - Keep the response concise and practical.

                                    Backend-calculated facts:

                                    %s
                                    """.formatted(structuredSummary))
                            .text(AIInsightContent.class)
                            .model(ChatModel.GPT_5_2)
                            .build();

            AIInsightContent content =
                    client.responses()
                            .create(params)
                            .output()
                            .stream()
                            .flatMap(item -> item.message().stream())
                            .flatMap(message -> message.content().stream())
                            .flatMap(contentItem -> contentItem.outputText().stream())
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "LLM returned no structured insight"
                                    )
                            );

            return objectMapper.writeValueAsString(content);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate AI weekly insight",
                    exception
            );
        }
    }
}