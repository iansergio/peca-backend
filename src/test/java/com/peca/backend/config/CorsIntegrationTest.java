package com.peca.backend.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * CORS com as origens de src/test/resources/application.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

	private static final String FRONT = "http://localhost:5173";
	private static final String OUTRA_ORIGEM = "https://site-malicioso.example";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions preflight(String url, String origem, String metodo) throws Exception {
		return mockMvc.perform(options(url)
				.header(HttpHeaders.ORIGIN, origem)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));
	}

	@Test
	void preflightDeOrigemPermitidaPassaSemToken() throws Exception {
		preflight("/api/pecas", FRONT, "POST")
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void preflightDeRotaProtegidaTambemPassaSemToken() throws Exception {
		preflight("/api/usuarios/" + UUID.randomUUID(), FRONT, "PUT")
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
	}

	@Test
	void origemNaoListadaNaoRecebeOHeader() throws Exception {
		preflight("/api/pecas", OUTRA_ORIGEM, "POST")
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		mockMvc.perform(get("/api/pecas").header(HttpHeaders.ORIGIN, OUTRA_ORIGEM))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void getPublicoDePecasComOrigemPermitidaDevolveOHeader() throws Exception {
		mockMvc.perform(get("/api/pecas").header(HttpHeaders.ORIGIN, FRONT))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString(HttpHeaders.RETRY_AFTER)))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString(HttpHeaders.LOCATION)));
	}

	@Test
	void rotaProtegidaContinuaExigindoToken() throws Exception {
		// O header CORS vem junto para o front conseguir ler o 401.
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.ORIGIN, FRONT)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
	}

}
