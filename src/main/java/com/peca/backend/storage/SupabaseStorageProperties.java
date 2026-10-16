package com.peca.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseStorageProperties(String url, String serviceKey, String bucket) {

	public boolean configurado() {
		return url != null && !url.isBlank() && serviceKey != null && !serviceKey.isBlank()
				&& bucket != null && !bucket.isBlank();
	}

	/**
	 * URL raiz do projeto. Tolera barra final e sufixos de API copiados por engano do painel
	 * (ex: {@code https://<ref>.supabase.co/rest/v1/}).
	 */
	public String urlBase() {
		return url.trim().replaceAll("/+$", "").replaceAll("/(rest|storage)/v1$", "");
	}

}
