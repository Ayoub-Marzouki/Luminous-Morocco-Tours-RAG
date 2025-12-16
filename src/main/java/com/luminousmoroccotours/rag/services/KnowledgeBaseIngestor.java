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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseIngestor {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${knowledge-base.path:classpath:knowledge-base}")
    private String knowledgeBasePath;

    @PostConstruct
    public void ingest() {
        log.info("Starting knowledge base ingestion...");

        // 1. Create Ingestor
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(1000, 200))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 2. Load Documents
        List<Document> documents = new ArrayList<>();
        
        try {
            // We need to resolve the path to a real file system path because FileSystemDocumentLoader needs it
            // This trick handles both IDE and jar (if unpacked) but for now let's assume local filesystem 
            // for the "src/main/resources" structure during dev.
            
            // For this specific environment (CLI dev), we can use relative paths
            File kbDir = new File("src/main/resources/knowledge-base");
            if (!kbDir.exists()) {
                 log.warn("Knowledge base directory not found at: {}", kbDir.getAbsolutePath());
                 return;
            }

            // Load PDFs from documents/
            File pdfDir = new File(kbDir, "documents");
            if (pdfDir.exists()) {
                 log.info("Loading PDFs from: {}", pdfDir.getAbsolutePath());
                 // ApachePdfBoxDocumentParser comes with langchain4j-spring-boot-starter usually, 
                 // or we might need to add it if it fails. Let's assume it's there.
                 // Actually, it's safer to use the generic loader if possible, but let's try explicit first.
                 try {
                     List<Document> pdfs = FileSystemDocumentLoader.loadDocuments(pdfDir.toPath(), new ApachePdfBoxDocumentParser());
                     documents.addAll(pdfs);
                     log.info("Loaded {} PDF documents.", pdfs.size());
                 } catch (Exception e) {
                     log.error("Failed to load PDFs (missing dependency?): {}", e.getMessage());
                 }
            }

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
                                 documents.add(doc);
                             } catch (IOException e) {
                                 log.error("Failed to load HTML file: {}", p, e);
                             }
                         });
                }
                log.info("Loaded HTML documents.");
            }

            if (!documents.isEmpty()) {
                ingestor.ingest(documents);
                log.info("Ingestion complete. {} documents ingested into memory.", documents.size());
            } else {
                log.warn("No documents found to ingest.");
            }

        } catch (Exception e) {
            log.error("Error during ingestion", e);
        }
    }

    private Document loadHtmlDocument(Path path) throws IOException {
        String html = Files.readString(path, StandardCharsets.UTF_8);
        // Clean HTML using Jsoup
        String text = Jsoup.parse(html).text();
        return Document.from(text, dev.langchain4j.data.document.Metadata.from("file_name", path.getFileName().toString()));
    }
}
