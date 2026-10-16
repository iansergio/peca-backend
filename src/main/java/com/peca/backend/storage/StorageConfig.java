package com.peca.backend.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class StorageConfig {

	@Bean
	public RestClient supabaseStorageRestClient() {
		return RestClient.create();
	}

}
