package com.naga.ai.requirement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class ChatClientConfig {
    private static final Logger logger =
            LoggerFactory.getLogger(ChatClientConfig.class);

    @Value("classpath:prompt/requirement-analysis-system.txt")
    private Resource systemPromptTemplate;

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            ChatMemory chatMemory) throws IOException {
        logger.info("Initialising ChatClient with " +
                        "model: {} and conversation memory",
                chatModel.getClass().getSimpleName());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptTemplate.getContentAsString(StandardCharsets.UTF_8))
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
