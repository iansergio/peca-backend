package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.auth.dto.SolicitarCodigoResponse;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.auth.dto.VerificarCodigoRequest;
import com.peca.backend.email.EmailService;
import com.peca.backend.email.EnvioEmailException;
import com.peca.backend.exception.LimiteExcedidoException;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import com.peca.backend.usuario.UsuarioService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant AGORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final LocalDateTime AGORA_LOCAL = LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC);

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private CodigoVerificacaoRepository codigoRepository;

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private EmailService emailService;

	private final SecurityConfig config = new SecurityConfig();
	private final SecretKey chave = new SecretKeySpec(
			"segredo-de-teste-com-pelo-menos-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	private final JwtDecoder decoder = config.jwtDecoder(chave);

	private final OtpProperties otpProperties = new OtpProperties(Duration.ofSeconds(60), 5, 20);
	private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(usuarioRepository, usuarioService, codigoRepository, config.jwtEncoder(chave),
				emailService, new JwtProperties("ignorado", Duration.ofHours(2)), otpProperties,
				new LimitadorSolicitacoesPorIp(otpProperties, clock), clock);
	}

	private SolicitarCodigoResponse solicitar(String nome, String email) {
		return service.solicitarCodigo(new SolicitarCodigoRequest(nome, email), "203.0.113.10");
	}

	// Pedidos anteriores do email, criados ha N segundos (do mais antigo para o mais novo).
	private void pedidosAnteriores(long... segundosAtras) {
		List<CodigoVerificacao> recentes = new ArrayList<>();
		for (long segundos : segundosAtras) {
			CodigoVerificacao codigo = new CodigoVerificacao();
			codigo.setCriadoEm(AGORA_LOCAL.minusSeconds(segundos));
			recentes.add(codigo);
		}
		when(codigoRepository.findByEmailAndCriadoEmAfterOrderByCriadoEmAsc("maria@email.com", AGORA_LOCAL.minusHours(1)))
				.thenReturn(recentes);
	}

	private static Usuario usuario(UUID id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Maria Silva");
		usuario.setEmail("maria@email.com");
		return usuario;
	}

	private static CodigoVerificacao codigo(Usuario usuario, String valor, LocalDateTime expiraEm) {
		CodigoVerificacao codigo = new CodigoVerificacao();
		codigo.setUsuario(usuario);
		codigo.setEmail(usuario.getEmail());
		codigo.setCodigo(valor);
		codigo.setExpiraEm(expiraEm);
		return codigo;
	}

	@Test
	void solicitarCodigoParaEmailNovoNaoCadastraUsuarioEGuardaONome() {
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());

		SolicitarCodigoResponse response = solicitar(" Maria Silva ", " Maria@Email.com ");

		verify(usuarioRepository, never()).save(any());
		verify(usuarioService, never()).cadastrar(any(), any());

		ArgumentCaptor<CodigoVerificacao> codigo = ArgumentCaptor.forClass(CodigoVerificacao.class);
		verify(codigoRepository).save(codigo.capture());
		assertThat(codigo.getValue().getCodigo()).matches("\\d{6}");
		assertThat(codigo.getValue().getUsuario()).isNull();
		assertThat(codigo.getValue().getNome()).isEqualTo("Maria Silva");
		assertThat(codigo.getValue().getEmail()).isEqualTo("maria@email.com");
		assertThat(codigo.getValue().getCriadoEm()).isEqualTo(AGORA_LOCAL);
		assertThat(codigo.getValue().getExpiraEm()).isEqualTo(AGORA_LOCAL.plusMinutes(10));
		assertThat(codigo.getValue().isUtilizado()).isFalse();
		assertThat(response.expiraEm()).isEqualTo(AGORA_LOCAL.plusMinutes(10));
		verify(emailService).enviarCodigoOtp("maria@email.com", codigo.getValue().getCodigo());
	}

	@Test
	void loginSemNomeDeUsuarioExistenteEnviaOCodigo() {
		Usuario existente = usuario(UUID.randomUUID());
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));

		solicitar(null, "maria@email.com");

		ArgumentCaptor<CodigoVerificacao> codigo = ArgumentCaptor.forClass(CodigoVerificacao.class);
		verify(codigoRepository).save(codigo.capture());
		assertThat(codigo.getValue().getUsuario()).isSameAs(existente);
		assertThat(codigo.getValue().getNome()).isNull();
		verify(emailService).enviarCodigoOtp("maria@email.com", codigo.getValue().getCodigo());
	}

	@Test
	void loginSemNomeDeEmailSemContaRespondeIgualSemGerarNemEnviarCodigo() {
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());

		SolicitarCodigoResponse response = solicitar("  ", "maria@email.com");

		assertThat(response.mensagem()).isEqualTo("Codigo enviado para o email informado");
		assertThat(response.expiraEm()).isEqualTo(AGORA_LOCAL.plusMinutes(10));
		verify(codigoRepository, never()).save(any());
		verify(emailService, never()).enviarCodigoOtp(any(), any());
	}

	@Test
	void falhaNoEnvioDoEmailDevePropagarErro() {
		Usuario existente = usuario(UUID.randomUUID());
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));
		doThrow(new EnvioEmailException("falhou", null)).when(emailService).enviarCodigoOtp(any(), any());

		assertThatThrownBy(() -> solicitar("Outro Nome", "maria@email.com"))
				.isInstanceOf(EnvioEmailException.class);
	}

	@Test
	void solicitarCodigoDeveVincularUsuarioExistenteEInvalidarCodigosAnteriores() {
		Usuario existente = usuario(UUID.randomUUID());
		CodigoVerificacao anterior = codigo(existente, "111111", AGORA_LOCAL.plusMinutes(5));
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));
		when(codigoRepository.findByEmailAndUtilizadoFalse("maria@email.com")).thenReturn(List.of(anterior));

		solicitar("Outro Nome", "maria@email.com");

		ArgumentCaptor<CodigoVerificacao> codigo = ArgumentCaptor.forClass(CodigoVerificacao.class);
		verify(codigoRepository).save(codigo.capture());
		assertThat(codigo.getValue().getUsuario()).isSameAs(existente);
		assertThat(codigo.getValue().getNome()).isNull();
		verify(usuarioRepository, never()).save(any());
		assertThat(existente.getNome()).isEqualTo("Maria Silva");
		assertThat(anterior.isUtilizado()).isTrue();
	}

	@Test
	void solicitarCodigoAntesDoIntervaloMinimoRetornaLimiteSemEnviarEmail() {
		pedidosAnteriores(20);

		assertThatThrownBy(() -> solicitar("Maria Silva", "maria@email.com"))
				.isInstanceOf(LimiteExcedidoException.class)
				.extracting("segundosParaNovaTentativa").isEqualTo(40L);
		verify(codigoRepository, never()).save(any());
		verify(emailService, never()).enviarCodigoOtp(any(), any());
	}

	@Test
	void solicitarCodigoDepoisDoIntervaloMinimoFunciona() {
		pedidosAnteriores(60);
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());

		solicitar("Maria Silva", "maria@email.com");

		verify(emailService).enviarCodigoOtp(eq("maria@email.com"), any());
	}

	@Test
	void solicitarCodigoAlemDoLimitePorHoraRetornaLimiteAteOPedidoMaisAntigoSairDaJanela() {
		// 5 pedidos na ultima hora; o mais antigo foi ha 50 minutos, entao libera em 10 minutos.
		pedidosAnteriores(3000, 2400, 1800, 1200, 600);

		assertThatThrownBy(() -> solicitar("Maria Silva", "maria@email.com"))
				.isInstanceOf(LimiteExcedidoException.class)
				.extracting("segundosParaNovaTentativa").isEqualTo(600L);
		verify(codigoRepository, never()).save(any());
		verify(emailService, never()).enviarCodigoOtp(any(), any());
	}

	@Test
	void solicitarCodigoAbaixoDoLimitePorHoraFunciona() {
		pedidosAnteriores(3000, 2400, 1800, 1200);
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());

		solicitar("Maria Silva", "maria@email.com");

		verify(emailService).enviarCodigoOtp(eq("maria@email.com"), any());
	}

	@Test
	void solicitarCodigoAlemDoLimitePorIpRetornaLimite() {
		when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
		for (int i = 0; i < 20; i++) {
			solicitar("Maria Silva", "maria" + i + "@email.com");
		}

		assertThatThrownBy(() -> solicitar("Maria Silva", "outra@email.com"))
				.isInstanceOf(LimiteExcedidoException.class);
		verify(emailService, never()).enviarCodigoOtp(eq("outra@email.com"), any());
	}

	@Test
	void verificarCodigoDeEmailNovoCadastraOUsuarioComONomeGuardado() {
		Usuario novo = usuario(UUID.randomUUID());
		CodigoVerificacao codigo = codigoSemUsuario("123456");
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigo));
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());
		when(usuarioService.cadastrar("Maria Silva", "maria@email.com")).thenReturn(novo);

		TokenResponse response = service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "123456"));

		assertThat(response.usuario().id()).isEqualTo(novo.getId());
		assertThat(codigo.getUsuario()).isSameAs(novo);
		assertThat(codigo.isUtilizado()).isTrue();
	}

	@Test
	void verificarCodigoErradoDeEmailNovoNaoCadastraUsuario() {
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigoSemUsuario("123456")));

		assertThatThrownBy(() -> service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "654321")))
				.isInstanceOf(CodigoInvalidoException.class);
		verify(usuarioService, never()).cadastrar(any(), any());
	}

	@Test
	void verificarCodigoDeEmailNovoReaproveitaUsuarioCriadoPorOutraVerificacao() {
		Usuario existente = usuario(UUID.randomUUID());
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigoSemUsuario("123456")));
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty(), Optional.of(existente));
		when(usuarioService.cadastrar(any(), any())).thenThrow(new DataIntegrityViolationException("email duplicado"));

		TokenResponse response = service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "123456"));

		assertThat(response.usuario().id()).isEqualTo(existente.getId());
		assertThat(response.usuario().nome()).isEqualTo("Maria Silva");
	}

	private static CodigoVerificacao codigoSemUsuario(String valor) {
		CodigoVerificacao codigo = new CodigoVerificacao();
		codigo.setEmail("maria@email.com");
		codigo.setNome("Maria Silva");
		codigo.setCodigo(valor);
		codigo.setExpiraEm(AGORA_LOCAL.plusMinutes(10));
		return codigo;
	}

	@Test
	void verificarCodigoValidoDeveEmitirTokenEMarcarComoUtilizado() {
		UUID id = UUID.randomUUID();
		CodigoVerificacao codigo = codigo(usuario(id), "123456", AGORA_LOCAL.plusMinutes(10));
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigo));

		TokenResponse response = service.verificarCodigo(new VerificarCodigoRequest("Maria@email.com", "123456"));

		Jwt jwt = decoder.decode(response.token());
		assertThat(jwt.getSubject()).isEqualTo(id.toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("maria@email.com");
		assertThat(jwt.getClaimAsString("nome")).isEqualTo("Maria Silva");
		assertThat(response.usuario().id()).isEqualTo(id);
		assertThat(response.tipo()).isEqualTo("Bearer");
		assertThat(response.expiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(2)));
		assertThat(codigo.isUtilizado()).isTrue();
		verify(usuarioService, never()).cadastrar(any(), any());
	}

	@Test
	void verificarCodigoErradoDeveFalharEContarTentativa() {
		CodigoVerificacao codigo = codigo(usuario(UUID.randomUUID()), "123456", AGORA_LOCAL.plusMinutes(10));
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigo));

		assertThatThrownBy(() -> service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "654321")))
				.isInstanceOf(CodigoInvalidoException.class);
		assertThat(codigo.getTentativas()).isEqualTo(1);
		assertThat(codigo.isUtilizado()).isFalse();
	}

	@Test
	void codigoDeveSerInvalidadoAposExcederTentativas() {
		CodigoVerificacao codigo = codigo(usuario(UUID.randomUUID()), "123456", AGORA_LOCAL.plusMinutes(10));
		codigo.setTentativas(AuthService.MAX_TENTATIVAS - 1);
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigo));

		assertThatThrownBy(() -> service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "654321")))
				.isInstanceOf(CodigoInvalidoException.class);
		assertThat(codigo.isUtilizado()).isTrue();
	}

	@Test
	void verificarCodigoExpiradoDeveFalhar() {
		CodigoVerificacao codigo = codigo(usuario(UUID.randomUUID()), "123456", AGORA_LOCAL.minusSeconds(1));
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.of(codigo));

		assertThatThrownBy(() -> service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "123456")))
				.isInstanceOf(CodigoInvalidoException.class);
		assertThat(codigo.isUtilizado()).isTrue();
	}

	@Test
	void verificarSemCodigoPendenteDeveFalhar() {
		when(codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc("maria@email.com"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.verificarCodigo(new VerificarCodigoRequest("maria@email.com", "123456")))
				.isInstanceOf(CodigoInvalidoException.class);
	}

	@Test
	void segredoCurtoDeveImpedirAInicializacao() {
		assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("curto", Duration.ofHours(2))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

}
