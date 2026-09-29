package com.insurance.assistant.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

// This app talks to two different databases: MySQL for the application
// data (users/policies/claims) and Postgres+pgvector for document
// embeddings. Spring Boot only auto-configures one DataSource for you, so
// once a second one is needed, both have to be defined explicitly - this is
// the standard "two DataSources" pattern from Spring Boot's own docs.
// The MySQL one is marked @Primary since JPA/Hibernate needs a single,
// unambiguous DataSource to build the EntityManagerFactory from.
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource primaryDataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("app.vector-datasource")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
