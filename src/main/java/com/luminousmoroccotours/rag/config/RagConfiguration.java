package com.luminousmoroccotours.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RagConfiguration {

    @Value("${langchain4j.google-ai-gemini.chat-model.api-key}")
    private String geminiApiKey;

    @Value("${vector-store.host}")
    private String host;

    @Value("${vector-store.port}")
    private int port;

    @Value("${vector-store.database}")
    private String database;

    @Value("${vector-store.user}")
    private String user;

    @Value("${vector-store.password}")
    private String password;

    @Value("${vector-store.dimension}")
    private int dimension;

    @Value("${vector-store.facts-table}")
    private String factsTable;

    @Value("${vector-store.strategy-table}")
    private String strategyTable;

    @Bean
    public GoogleAiGeminiChatModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-2.5-flash-lite")
                .temperature(0.7)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    /**
     * Facts Store: Contains ONLY HTMLs from raw-data (company website ground truth).
     * Used by RagService (Chatbot) to answer factual questions.
     */
    @Bean
    @Qualifier("factsStore")
    @Primary
    public EmbeddingStore<TextSegment> factsEmbeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(factsTable)
                .dimension(dimension)
                .build();
    }

    /**
     * Strategy Store: Contains ONLY PDFs from documents (strategy docs, competitor analysis).
     * Used by BlogService to write SEO-optimized blogs with strategic voice.
     */
    @Bean
    @Qualifier("strategyStore")
    public EmbeddingStore<TextSegment> strategyEmbeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(strategyTable)
                .dimension(dimension)
                .build();
    }

    /**
     * Content Retriever for Facts Store.
     * Used by RagService to retrieve company information from website HTMLs.
     */
    @Bean
    @Qualifier("factsRetriever")
    @Primary
    public ContentRetriever factsContentRetriever(
            @Qualifier("factsStore") EmbeddingStore<TextSegment> factsStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(factsStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.6)
                .build();
    }

    /**
     * Content Retriever for Strategy Store.
     * Used by BlogService to retrieve style guides and competitor analysis.
     */
    @Bean
    @Qualifier("strategyRetriever")
    public ContentRetriever strategyContentRetriever(
            @Qualifier("strategyStore") EmbeddingStore<TextSegment> strategyStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(strategyStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();
    }
}
