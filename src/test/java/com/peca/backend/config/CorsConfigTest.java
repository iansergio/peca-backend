package com.peca.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

	private static CorsConfiguration configuracao(List<String> origens) {
		return new CorsConfig().corsConfigurationSource(new CorsProperties(origens))
				.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/pecas"));
	}

	@Test
	void semOrigensConfiguradasNenhumaOrigemELiberada() {
		assertThat(configuracao(null).checkOrigin("http://localhost:5173")).isNull();
		assertThat(configuracao(List.of("")).checkOrigin("http://localhost:5173")).isNull();
	}

	@Test
	void origensSaoComparadasExatamenteEIgnoramEspacos() {
		CorsConfiguration config = configuracao(List.of(" https://peca.app ", "http://localhost:5173"));

		assertThat(config.checkOrigin("https://peca.app")).isEqualTo("https://peca.app");
		assertThat(config.checkOrigin("https://peca.app.malicioso.example")).isNull();
	}

	@Test
	void curingaNaoEPermitido() {
		assertThatThrownBy(() -> new CorsProperties(List.of("*")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CORS_ALLOWED_ORIGINS");
	}

}
