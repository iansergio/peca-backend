package com.peca.backend.storage;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Armazenamento das imagens no disco local (no container, um volume).
 *
 * @param diretorio pasta onde as imagens sao gravadas
 * @param urlPublica prefixo das URLs devolvidas em imagemUrl, sem barra final (ex: /imagens, a rota do front-end)
 */
@ConfigurationProperties(prefix = "app.armazenamento")
public record ArmazenamentoProperties(Path diretorio, String urlPublica) {

	public ArmazenamentoProperties {
		if (diretorio == null) {
			throw new IllegalStateException("STORAGE_DIR nao definido: informe a pasta onde as imagens sao gravadas");
		}
		urlPublica = urlPublica == null ? "" : urlPublica.trim().replaceAll("/+$", "");
	}

}
