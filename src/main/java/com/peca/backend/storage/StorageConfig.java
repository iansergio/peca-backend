package com.peca.backend.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ArmazenamentoProperties.class)
public class StorageConfig {
}
