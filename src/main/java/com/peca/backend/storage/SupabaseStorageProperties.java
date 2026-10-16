package com.peca.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseStorageProperties(String url, String serviceKey, String bucket) {

	public boolean configurado() {
		return url != null && !url.isBlank() && serviceKey != null && !serviceKey.isBlank()
				&& bucket != null && !bucket.isBlank();
	}

	public String urlBase() {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

}
