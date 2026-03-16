package com.naga.ai.requirement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {
    private static final Logger logger = LoggerFactory.getLogger(ChatMemoryConfig.class);

    @Bean
    public ChatMemory chatMemory() {
        logger.info("Initialising InMemoryChatMemory " +
                "for conversation history");
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(
                        new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
