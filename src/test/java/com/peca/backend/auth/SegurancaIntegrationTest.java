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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Fluxo completo: cadastro integrado por OTP, rotas protegidas e checagem de autoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegrationTest {

	private static final String PECA = """
			{"titulo":"Tela iPhone 13","descricao":"Original","preco":450.00,"categoria":"Tela","modeloCelular":"iPhone 13"}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CodigoVerificacaoRepository codigoRepository;

	private record Sessao(String authorization, String usuarioId) {
	}

	private void solicitarCodigo(String email) throws Exception {
		mockMvc.perform(post("/api/auth/solicitar-codigo").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s"}""".formatted(email)))
				.andExpect(status().isOk());
	}

	private ResultActions verificarCodigo(String email, String codigo) throws Exception {
		return mockMvc.perform(post("/api/auth/verificar-codigo").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","codigo":"%s"}""".formatted(email, codigo)));
	}

	private String codigoAtual(String email) {
		return codigoRepository.findByEmailAndUtilizadoFalse(email).getFirst().getCodigo();
	}

	private Sessao cadastrarELogar(String email) throws Exception {
		solicitarCodigo(email);
		String resposta = verificarCodigo(email, codigoAtual(email))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.email").value(email))
				.andReturn().getResponse().getContentAsString();
		return new Sessao("Bearer " + JsonPath.read(resposta, "$.token"), JsonPath.read(resposta, "$.usuario.id"));
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
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Maria","email":"maria@email.com"}"""))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void codigoErradoOuReutilizadoRetorna401() throws Exception {
		String email = email();
		solicitarCodigo(email);
		String codigo = codigoAtual(email);
		String errado = codigo.equals("000000") ? "111111" : "000000";

		verificarCodigo(email, errado).andExpect(status().isUnauthorized());
		verificarCodigo(email, codigo).andExpect(status().isOk());
		verificarCodigo(email, codigo).andExpect(status().isUnauthorized());
	}

	@Test
	void loginPorSenhaNaoExisteMais() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"a@email.com","senha":"qualquer"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void somenteOVendedorPodeAlterarOAnuncio() throws Exception {
		String dono = cadastrarELogar(email()).authorization();
		String outro = cadastrarELogar(email()).authorization();

		String criada = mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, dono)
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vendedor.nome").isNotEmpty())
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
