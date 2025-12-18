package com.luminousmoroccotours.rag.config;

import com.luminousmoroccotours.rag.services.BlogService;
import com.luminousmoroccotours.rag.services.RagService;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI Services with explicit retriever binding.
 * This ensures:
 * - RagService (Chatbot) uses ONLY factsRetriever (embeddings_facts table)
 * - BlogService (Generator) uses ONLY strategyRetriever (embeddings_strategy table)
 */
@Configuration
public class AiServicesConfiguration {

    /**
     * RagService (Chatbot) - Connected to Facts Store.
     * Can ONLY see company website HTMLs.
     */
    @Bean
    public RagService ragService(
            GoogleAiGeminiChatModel chatModel,
            @Qualifier("factsRetriever") ContentRetriever factsRetriever) {
        return AiServices.builder(RagService.class)
                .chatModel(chatModel)
                .contentRetriever(factsRetriever)
                .build();
    }

    /**
     * BlogService (Generator) - Connected to Strategy Store.
     * Can see strategy docs and competitor analysis.
     */
    @Bean
    public BlogService blogService(
            GoogleAiGeminiChatModel chatModel,
            @Qualifier("strategyRetriever") ContentRetriever strategyRetriever) {
        return AiServices.builder(BlogService.class)
                .chatModel(chatModel)
                .contentRetriever(strategyRetriever)
                .build();
    }
}
