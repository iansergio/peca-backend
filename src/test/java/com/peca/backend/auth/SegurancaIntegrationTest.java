package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.email.EmailService;
import com.peca.backend.email.EnvioEmailException;
import com.peca.backend.usuario.UsuarioRepository;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fluxo completo: cadastro integrado por OTP, rotas protegidas e checagem de autoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegrationTest {

	private static final String PECA = """
			{"titulo":"Tela iPhone 13","descricao":"Original","preco":450.00,"categoria":"Tela","modeloCelular":"iPhone 13","whatsapp":"11999998888"}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CodigoVerificacaoRepository codigoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AuthService authService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	// Evita conexao SMTP real; o codigo e lido direto do banco.
	@MockitoBean
	private EmailService emailService;

	private record Sessao(String authorization, String usuarioId) {
	}

	// Cada pedido usa um IP proprio (via X-Forwarded-For) para nao esbarrar no limite por IP entre testes.
	private ResultActions solicitarCodigo(String email, String ip) throws Exception {
		return mockMvc.perform(post("/api/auth/solicitar-codigo").contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", ip)
				.content("""
						{"nome":"Maria Silva","email":"%s"}""".formatted(email)));
	}

	private void solicitarCodigo(String email) throws Exception {
		solicitarCodigo(email, ipAleatorio()).andExpect(status().isOk());
	}

	private static String ipAleatorio() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
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

	// Recua os pedidos do email para passar do intervalo minimo entre pedidos.
	private void recuarPedidos(String email) {
		codigoRepository.findAll().stream().filter(codigo -> codigo.getEmail().equals(email)).forEach(codigo -> {
			codigo.setCriadoEm(codigo.getCriadoEm().minusMinutes(2));
			codigoRepository.save(codigo);
		});
	}

	private static String email() {
		return UUID.randomUUID() + "@email.com";
	}

	@Test
	void rotasPublicasNaoExigemToken() throws Exception {
		mockMvc.perform(get("/api/pecas")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		// Foto inexistente: 404, e nao 401 (a rota de imagens e publica).
		mockMvc.perform(get("/api/imagens/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rotasDeEscritaExigemToken() throws Exception {
		mockMvc.perform(post("/api/pecas").contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/pecas/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/usuarios/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Maria","email":"maria@email.com"}"""))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions solicitarCodigoSemNome(String email) throws Exception {
		return mockMvc.perform(post("/api/auth/solicitar-codigo").contentType(MediaType.APPLICATION_JSON)
				.header("X-Forwarded-For", ipAleatorio())
				.content("""
						{"email":"%s"}""".formatted(email)));
	}

	@Test
	void loginSemNomeDeUsuarioCadastradoEnviaCodigoValido() throws Exception {
		String email = email();
		cadastrarELogar(email);
		recuarPedidos(email);

		solicitarCodigoSemNome(email).andExpect(status().isOk());
		verificarCodigo(email, codigoAtual(email))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.nome").value("Maria Silva"));
	}

	@Test
	void loginSemNomeDeEmailSemContaRespondeOkSemGerarCodigo() throws Exception {
		String email = email();
		solicitarCodigoSemNome(email).andExpect(status().isOk());
		assertThat(codigoRepository.findByEmailAndUtilizadoFalse(email)).isEmpty();
		assertThat(usuarioRepository.findByEmail(email)).isEmpty();
	}

	@Test
	void solicitarCodigoComEmailInvalidoRetorna400() throws Exception {
		solicitarCodigoSemNome("nao-e-email").andExpect(status().isBadRequest());
	}

	@Test
	void cadastroPorOtpUsaONomeInformado() throws Exception {
		String email = email();
		solicitarCodigo(email);
		verificarCodigo(email, codigoAtual(email))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.nome").value("Maria Silva"));
	}

	@Test
	void solicitarCodigoSemVerificarNaoCriaUsuario() throws Exception {
		String email = email();
		solicitarCodigo(email);

		assertThat(usuarioRepository.findByEmail(email)).isEmpty();
		verificarCodigo(email, codigoAtual(email)).andExpect(status().isOk());
		assertThat(usuarioRepository.findByEmail(email)).isPresent();
	}

	@Test
	void novoPedidoAntesDoIntervaloMinimoRetorna429ComRetryAfter() throws Exception {
		String email = email();
		solicitarCodigo(email);
		String codigo = codigoAtual(email);

		solicitarCodigo(email, ipAleatorio())
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("\\d+")))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.detail").value(startsWith("Muitas solicitacoes")));
		// O pedido recusado nao gera codigo novo: o anterior continua valido.
		assertThat(codigoAtual(email)).isEqualTo(codigo);
	}

	@Test
	void limitePorIpRetorna429SemRevelarSeOEmailExiste() throws Exception {
		String ip = ipAleatorio();
		String cadastrado = email();
		cadastrarELogar(cadastrado);
		for (int i = 0; i < 20; i++) {
			solicitarCodigo(email(), ip).andExpect(status().isOk());
		}

		String novo = solicitarCodigo(email(), ip).andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andReturn().getResponse().getContentAsString();
		String existente = solicitarCodigo(cadastrado, ip).andExpect(status().isTooManyRequests())
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(existente, "$.detail")).isEqualTo(JsonPath.read(novo, "$.detail"));
	}

	// R-3, AC-14: mesma sequencia de respostas com e sem conta.
	@Test
	void limitePorEmailNaoRevelaSeOEmailTemConta() throws Exception {
		String cadastrado = email();
		cadastrarELogar(cadastrado);
		recuarPedidos(cadastrado);
		String semConta = email();

		String corpoComConta = solicitarCodigoSemNome(cadastrado).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String corpoSemConta = solicitarCodigoSemNome(semConta).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(corpoSemConta, "$.mensagem"))
				.isEqualTo(JsonPath.read(corpoComConta, "$.mensagem"));

		String recusaComConta = solicitarCodigoSemNome(cadastrado).andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andReturn().getResponse().getContentAsString();
		String recusaSemConta = solicitarCodigoSemNome(semConta).andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<Integer>read(recusaSemConta, "$.status"))
				.isEqualTo(JsonPath.<Integer>read(recusaComConta, "$.status"));
	}

	// R-8, AC-16, AC-19
	@Test
	void pedidoComNomeLogoDepoisDePedidoSemContaRetorna429() throws Exception {
		String email = email();
		solicitarCodigoSemNome(email).andExpect(status().isOk());
		assertThat(codigoRepository.findByEmailAndUtilizadoFalse(email)).isEmpty();

		solicitarCodigo(email, ipAleatorio()).andExpect(status().isTooManyRequests());
		assertThat(codigoRepository.findByEmailAndUtilizadoFalse(email)).isEmpty();
		verify(emailService, after(200).never()).enviarCodigoOtp(eq(email), any());
	}

	// R-3, AC-20: 200 e 429, com e sem conta, nunca respondem antes do tempo minimo.
	@Test
	void respostasDoPedidoRespeitamOTempoMinimo() throws Exception {
		String email = email();

		for (int status : new int[] { 200, 429 }) {
			long inicio = System.nanoTime();
			solicitarCodigoSemNome(email).andExpect(status().is(status));
			assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isGreaterThanOrEqualTo(TempoMinimoResposta.MINIMO);
		}
	}

	// R-11, AC-17: a resposta chega com o SMTP travado.
	@Test
	void respostaNaoEsperaOEnvioDoEmail() throws Exception {
		String email = email();
		CountDownLatch liberarSmtp = new CountDownLatch(1);
		CountDownLatch envioConcluido = new CountDownLatch(1);
		AtomicBoolean liberadoDepoisDaResposta = new AtomicBoolean();
		doAnswer(chamada -> {
			liberadoDepoisDaResposta.set(liberarSmtp.await(5, TimeUnit.SECONDS));
			envioConcluido.countDown();
			return null;
		}).when(emailService).enviarCodigoOtp(eq(email), any());

		solicitarCodigo(email, ipAleatorio()).andExpect(status().isOk());
		liberarSmtp.countDown();

		assertThat(envioConcluido.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(liberadoDepoisDaResposta).isTrue();
	}

	// R-11, AC-11
	@Test
	void falhaNoEnvioDoEmailResponde200EMantemOCodigo() throws Exception {
		String email = email();
		doThrow(new EnvioEmailException("falhou", null)).when(emailService).enviarCodigoOtp(eq(email), any());

		solicitarCodigo(email, ipAleatorio()).andExpect(status().isOk());

		verify(emailService, timeout(5000)).enviarCodigoOtp(eq(email), any());
		assertThat(codigoRepository.findByEmailAndUtilizadoFalse(email)).hasSize(1);
	}

	// R-11, AC-18
	@Test
	void transacaoDesfeitaNaoEnviaEmail() {
		String email = email();

		transactionTemplate.executeWithoutResult(transacao -> {
			authService.solicitarCodigo(new SolicitarCodigoRequest("Maria Silva", email), ipAleatorio());
			transacao.setRollbackOnly();
		});

		verify(emailService, after(200).never()).enviarCodigoOtp(eq(email), any());
		assertThat(codigoRepository.findByEmailAndUtilizadoFalse(email)).isEmpty();
	}

	@Test
	void anuncioSemNumeroDeContatoRetorna400() throws Exception {
		String dono = cadastrarELogar(email()).authorization();
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, dono)
						.contentType(MediaType.APPLICATION_JSON)
						.content(PECA.replace(",\"whatsapp\":\"11999998888\"", "")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/pecas").header(HttpHeaders.AUTHORIZATION, dono)
						.contentType(MediaType.APPLICATION_JSON).content(PECA))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vendedor.whatsapp").value("11999998888"));
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
	void usuarioSoLeOProprioPerfil() throws Exception {
		String emailMaria = email();
		Sessao maria = cadastrarELogar(emailMaria);
		Sessao joao = cadastrarELogar(email());

		mockMvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, maria.authorization()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(maria.usuarioId()))
				.andExpect(jsonPath("$.email").value(emailMaria));
		mockMvc.perform(get("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, maria.authorization()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(emailMaria));
		mockMvc.perform(get("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, joao.authorization()))
				.andExpect(status().isForbidden())
				.andExpect(content().string(not(containsString(emailMaria))));
	}

	@Test
	void edicaoDePerfilNaoAlteraOEmail() throws Exception {
		String emailMaria = email();
		Sessao maria = cadastrarELogar(emailMaria);
		Sessao joao = cadastrarELogar(email());
		String perfil = """
				{"nome":"Maria Souza","email":"%s","whatsapp":"11988887777"}""".formatted(email());

		mockMvc.perform(put("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, maria.authorization())
						.contentType(MediaType.APPLICATION_JSON).content(perfil))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Maria Souza"))
				.andExpect(jsonPath("$.whatsapp").value("11988887777"))
				.andExpect(jsonPath("$.email").value(emailMaria));
		mockMvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, maria.authorization()))
				.andExpect(jsonPath("$.email").value(emailMaria));
		mockMvc.perform(put("/api/usuarios/" + maria.usuarioId()).header(HttpHeaders.AUTHORIZATION, joao.authorization())
						.contentType(MediaType.APPLICATION_JSON).content(perfil))
				.andExpect(status().isForbidden());
	}

	@Test
	void listagemDeUsuariosNaoExisteMais() throws Exception {
		String joao = cadastrarELogar(email()).authorization();

		mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, joao))
				.andExpect(status().isNotFound());
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
