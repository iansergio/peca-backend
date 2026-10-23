package com.peca.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origens do front-end autorizadas a chamar a API pelo navegador (CORS).
 *
 * @param origensPermitidas origens exatas (ex: https://peca.app); vazio = nenhuma origem liberada
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> origensPermitidas) {

	public CorsProperties {
		origensPermitidas = origensPermitidas == null ? List.of()
				: origensPermitidas.stream().map(String::trim).filter(origem -> !origem.isEmpty()).toList();
		if (origensPermitidas.contains("*")) {
			throw new IllegalStateException(
					"CORS_ALLOWED_ORIGINS nao pode conter '*': liste as origens do front-end separadas por virgula");
		}
	}

}
