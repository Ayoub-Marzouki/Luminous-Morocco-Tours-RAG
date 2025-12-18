package com.luminousmoroccotours.rag.services;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class KnowledgeBaseIngestor {

    private final EmbeddingStore<TextSegment> factsStore;
    private final EmbeddingStore<TextSegment> strategyStore;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @Value("${knowledge-base.path:classpath:knowledge-base}")
    private String knowledgeBasePath;

    @Value("${vector-store.facts-table}")
    private String factsTable;

    @Value("${vector-store.strategy-table}")
    private String strategyTable;

    public KnowledgeBaseIngestor(
            @Qualifier("factsStore") EmbeddingStore<TextSegment> factsStore,
            @Qualifier("strategyStore") EmbeddingStore<TextSegment> strategyStore,
            EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate) {
        this.factsStore = factsStore;
        this.strategyStore = strategyStore;
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ingest() {
        log.info("=== Starting Separation of Concerns Ingestion (Nuke and Pave Mode) ===");

        try {
            // 0. Clear tables to prevent duplication
            log.info("Cleaning up old data from {} and {}...", factsTable, strategyTable);
            jdbcTemplate.execute("DELETE FROM " + factsTable);
            jdbcTemplate.execute("DELETE FROM " + strategyTable);

            // Resolve knowledge base directory
            File kbDir = new File("src/main/resources/knowledge-base");
            if (!kbDir.exists()) {
                log.warn("Knowledge base directory not found at: {}", kbDir.getAbsolutePath());
                return;
            }

            // ===== INGEST HTMLs into FACTS STORE =====
            ingestHtmlsIntoFactsStore(kbDir);

            // ===== INGEST PDFs into STRATEGY STORE =====
            ingestPdfsIntoStrategyStore(kbDir);

            log.info("=== Separation of Concerns Ingestion Complete ===");

        } catch (Exception e) {
            log.error("Error during ingestion", e);
        }
    }

    /**
     * Ingests HTML files from raw-data/ into the Facts Store.
     * This store contains ONLY the ground truth from the company website.
     */
    private void ingestHtmlsIntoFactsStore(File kbDir) {
        log.info(">>> Ingesting HTMLs into FACTS STORE (embeddings_facts table)");

        // Create Facts Ingestor
        EmbeddingStoreIngestor factsIngestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 200))
                .embeddingModel(embeddingModel)
                .embeddingStore(factsStore)
                .build();

        List<Document> htmlDocuments = new ArrayList<>();

        // Load HTMLs from raw-data/
        File htmlDir = new File(kbDir, "raw-data");
        if (htmlDir.exists()) {
            log.info("Loading HTMLs from: {}", htmlDir.getAbsolutePath());
            try (Stream<Path> paths = Files.walk(htmlDir.toPath())) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".html"))
                        .forEach(p -> {
                            try {
                                Document doc = loadHtmlDocument(p);
                                htmlDocuments.add(doc);
                            } catch (IOException e) {
                                log.error("Failed to load HTML file: {}", p, e);
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to walk HTML directory", e);
            }
            log.info("Loaded {} HTML documents.", htmlDocuments.size());
        }

        if (!htmlDocuments.isEmpty()) {
            factsIngestor.ingest(htmlDocuments);
            log.info("✓ Ingested {} HTML documents into FACTS STORE.", htmlDocuments.size());
        } else {
            log.warn("No HTML documents found to ingest into FACTS STORE.");
        }
    }

    /**
     * Ingests PDF files from documents/ into the Strategy Store.
     * This store contains strategy docs and competitor analysis.
     */
    private void ingestPdfsIntoStrategyStore(File kbDir) {
        log.info(">>> Ingesting PDFs into STRATEGY STORE (embeddings_strategy table)");

        // Create Strategy Ingestor
        EmbeddingStoreIngestor strategyIngestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 200))
                .embeddingModel(embeddingModel)
                .embeddingStore(strategyStore)
                .build();

        List<Document> pdfDocuments = new ArrayList<>();

        // Load PDFs from documents/
        File pdfDir = new File(kbDir, "documents");
        if (pdfDir.exists()) {
            log.info("Loading PDFs from: {}", pdfDir.getAbsolutePath());
            try {
                List<Document> pdfs = FileSystemDocumentLoader.loadDocuments(
                        pdfDir.toPath(), 
                        new ApachePdfBoxDocumentParser()
                );
                pdfDocuments.addAll(pdfs);
                log.info("Loaded {} PDF documents.", pdfs.size());
            } catch (Exception e) {
                log.error("Failed to load PDFs: {}", e.getMessage());
            }
        }

        if (!pdfDocuments.isEmpty()) {
            strategyIngestor.ingest(pdfDocuments);
            log.info("✓ Ingested {} PDF documents into STRATEGY STORE.", pdfDocuments.size());
        } else {
            log.warn("No PDF documents found to ingest into STRATEGY STORE.");
        }
    }

    private Document loadHtmlDocument(Path path) throws IOException {
        String html = Files.readString(path, StandardCharsets.UTF_8);
        // Clean HTML using Jsoup
        String text = Jsoup.parse(html).text();
        return Document.from(text, dev.langchain4j.data.document.Metadata.from("file_name", path.getFileName().toString()));
    }
}
