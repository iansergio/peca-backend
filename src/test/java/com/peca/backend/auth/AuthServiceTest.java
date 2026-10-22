package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
	private EmailService emailService;

	private final SecurityConfig config = new SecurityConfig();
	private final SecretKey chave = new SecretKeySpec(
			"segredo-de-teste-com-pelo-menos-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	private final JwtDecoder decoder = config.jwtDecoder(chave);
	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(usuarioRepository, codigoRepository, config.jwtEncoder(chave), emailService,
				new JwtProperties("ignorado", Duration.ofHours(2)), Clock.fixed(AGORA, ZoneOffset.UTC));
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
	void solicitarCodigoDeveCadastrarUsuarioNovoEGerarCodigoDe6Digitos() {
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.empty());
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

		SolicitarCodigoResponse response = service.solicitarCodigo(new SolicitarCodigoRequest(" Maria Silva ", " Maria@Email.com "));

		ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(usuario.capture());
		assertThat(usuario.getValue().getEmail()).isEqualTo("maria@email.com");
		assertThat(usuario.getValue().getNome()).isEqualTo("Maria Silva");

		ArgumentCaptor<CodigoVerificacao> codigo = ArgumentCaptor.forClass(CodigoVerificacao.class);
		verify(codigoRepository).save(codigo.capture());
		assertThat(codigo.getValue().getCodigo()).matches("\\d{6}");
		assertThat(codigo.getValue().getUsuario()).isSameAs(usuario.getValue());
		assertThat(codigo.getValue().getExpiraEm()).isEqualTo(AGORA_LOCAL.plusMinutes(10));
		assertThat(codigo.getValue().isUtilizado()).isFalse();
		assertThat(response.expiraEm()).isEqualTo(AGORA_LOCAL.plusMinutes(10));
		verify(emailService).enviarCodigoOtp("maria@email.com", codigo.getValue().getCodigo());
	}

	@Test
	void falhaNoEnvioDoEmailDevePropagarErro() {
		Usuario existente = usuario(UUID.randomUUID());
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));
		doThrow(new EnvioEmailException("falhou", null)).when(emailService).enviarCodigoOtp(any(), any());

		assertThatThrownBy(() -> service.solicitarCodigo(new SolicitarCodigoRequest("Outro Nome", "maria@email.com")))
				.isInstanceOf(EnvioEmailException.class);
	}

	@Test
	void solicitarCodigoDeveReaproveitarUsuarioEInvalidarCodigosAnteriores() {
		Usuario existente = usuario(UUID.randomUUID());
		CodigoVerificacao anterior = codigo(existente, "111111", AGORA_LOCAL.plusMinutes(5));
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(existente));
		when(codigoRepository.findByEmailAndUtilizadoFalse("maria@email.com")).thenReturn(List.of(anterior));

		service.solicitarCodigo(new SolicitarCodigoRequest("Outro Nome", "maria@email.com"));

		verify(usuarioRepository, never()).save(any());
		assertThat(existente.getNome()).isEqualTo("Maria Silva");
		assertThat(anterior.isUtilizado()).isTrue();
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
