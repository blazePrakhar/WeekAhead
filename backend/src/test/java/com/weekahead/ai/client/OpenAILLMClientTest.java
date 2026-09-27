package com.weekahead.ai.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;

class OpenAILLMClientTest {

    @Test
    void shouldCreateClient() {
        OpenAIClient client = org.mockito.Mockito.mock(OpenAIClient.class);

        OpenAILLMClient llmClient =
                new OpenAILLMClient(new ObjectMapper());

        assertNotNull(llmClient);
        assertNotNull(client);
    }
}