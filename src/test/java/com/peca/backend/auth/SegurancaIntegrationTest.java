package com.peca.backend.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fluxo completo: cadastro publico, login, rotas protegidas e checagem de autoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegrationTest {

	private static final String PECA = """
			{"titulo":"Tela iPhone 13","descricao":"Original","preco":450.00,"categoria":"Tela","modeloCelular":"iPhone 13"}""";

	@Autowired
	private MockMvc mockMvc;

	private record Sessao(String authorization, String usuarioId) {
	}

	private Sessao cadastrarELogar(String email) throws Exception {
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Usuario","email":"%s","senha":"senhaForte123","whatsapp":"11999998888"}""".formatted(email)))
				.andExpect(status().isCreated());
		String resposta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","senha":"senhaForte123"}""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return new Sessao("Bearer " + JsonPath.read(resposta, "$.token"), JsonPath.read(resposta, "$.usuarioId"));
	}

	private static String email() {
		return UUID.randomUUID() + "@email.com";
	}

	@Test
	void rotasPublicasNaoExigemToken() throws Exception {
		mockMvc.perform(get("/api/pecas")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void rotasDeEscritaExigemToken() throws Exception {
		mockMvc.perform(post("/api/pecas").contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/pecas/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginComSenhaErradaRetorna401() throws Exception {
		String email = email();
		cadastrarELogar(email);
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","senha":"errada123"}""".formatted(email)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void somenteOVendedorPodeAlterarOAnuncio() throws Exception {
		String dono = cadastrarELogar(email()).authorization();
		String outro = cadastrarELogar(email()).authorization();

		String criada = mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, dono)
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vendedor.nome").value("Usuario"))
				.andExpect(jsonPath("$.vendedor", not(hasKey("senha"))))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(criada, "$.id");

		mockMvc.perform(put("/api/pecas/" + id).header(HttpHeaders.AUTHORIZATION, outro)
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/pecas/" + id).header(HttpHeaders.AUTHORIZATION, outro))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/pecas/" + id).header(HttpHeaders.AUTHORIZATION, dono))
				.andExpect(status().isNoContent());
	}

	@Test
	void usuarioSoPodeRemoverAPropriaConta() throws Exception {
		Sessao maria = cadastrarELogar(email());
		Sessao joao = cadastrarELogar(email());

		mockMvc.perform(delete("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, joao.authorization()))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, maria.authorization()))
				.andExpect(status().isNoContent());
	}

}
