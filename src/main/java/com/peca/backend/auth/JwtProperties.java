package com.peca.backend.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration expiracao) {

	private static final Duration EXPIRACAO_PADRAO = Duration.ofHours(2);

	public JwtProperties {
		if (expiracao == null || expiracao.isZero() || expiracao.isNegative()) {
			expiracao = EXPIRACAO_PADRAO;
		}
	}

}
