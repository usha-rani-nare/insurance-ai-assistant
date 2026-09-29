package com.insurance.assistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    // all-MiniLM-L6-v2 (the local embedding model from
    // spring-ai-starter-model-transformers) produces 384-dimension vectors.
    // This must match the model in use - if the embedding model ever
    // changes, the vector_store table has to be recreated with the new
    // dimension.
    private static final int EMBEDDING_DIMENSIONS = 384;

    @Bean
    public PgVectorStore vectorStore(@Qualifier("vectorDataSource") DataSource vectorDataSource,
                                      EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(vectorDataSource);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(EMBEDDING_DIMENSIONS)
                .initializeSchema(true)
                .build();
    }
}
