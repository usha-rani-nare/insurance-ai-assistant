package com.insurance.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Loads the sample policy documents (src/main/resources/documents/*.txt)
// into the vector store on startup. This is a small, fixed set of files, so
// a CommandLineRunner is simpler and easier to explain than a separate
// admin endpoint or scheduled job.
@Component
public class DocumentIngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final String DOCUMENTS_LOCATION = "classpath:documents/*.txt";

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        if (alreadyIngested()) {
            log.info("Policy documents already ingested - skipping.");
            return;
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(DOCUMENTS_LOCATION);

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> allChunks = new ArrayList<>();

        for (Resource resource : resources) {
            TextReader reader = new TextReader(resource);
            // Keep a clean, plain filename in "source" metadata - this is
            // what the API returns to the caller - rather than the reader's
            // default value, which includes the full resource description.
            reader.getCustomMetadata().put("source", resource.getFilename());

            allChunks.addAll(splitter.apply(reader.get()));
        }

        vectorStore.add(allChunks);
        log.info("Ingested {} chunks from {} policy documents", allChunks.size(), resources.length);
    }

    // A cheap way to check "is the store already populated" without coupling
    // this to the vector table's schema directly - just search for anything
    // and see if a result comes back.
    private boolean alreadyIngested() {
        List<Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder().query("policy").topK(1).build());
        return !existing.isEmpty();
    }
}
