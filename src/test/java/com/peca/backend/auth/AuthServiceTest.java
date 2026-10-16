package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.peca.backend.auth.dto.LoginRequest;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant AGORA = Instant.now();

	@Mock
	private UsuarioRepository usuarioRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final SecurityConfig config = new SecurityConfig();
	private final SecretKey chave = new SecretKeySpec(
			"segredo-de-teste-com-pelo-menos-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	private final JwtDecoder decoder = config.jwtDecoder(chave);
	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(usuarioRepository, passwordEncoder, config.jwtEncoder(chave),
				new JwtProperties("ignorado", Duration.ofHours(2)), Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	private Usuario usuario(UUID id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Maria Silva");
		usuario.setEmail("maria@email.com");
		usuario.setSenha(passwordEncoder.encode("senhaForte123"));
		return usuario;
	}

	@Test
	void loginDeveGerarTokenValidoComIdDoUsuario() {
		UUID id = UUID.randomUUID();
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario(id)));

		TokenResponse response = service.login(new LoginRequest(" Maria@Email.com ", "senhaForte123"));

		Jwt jwt = decoder.decode(response.token());
		assertThat(jwt.getSubject()).isEqualTo(id.toString());
		assertThat(response.usuarioId()).isEqualTo(id);
		assertThat(response.tipo()).isEqualTo("Bearer");
		assertThat(response.expiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(2)));
	}

	@Test
	void loginDeveFalharComSenhaErrada() {
		when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario(UUID.randomUUID())));

		assertThatThrownBy(() -> service.login(new LoginRequest("maria@email.com", "senhaErrada")))
				.isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void loginDeveFalharComEmailInexistente() {
		when(usuarioRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@email.com", "qualquer")))
				.isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void segredoCurtoDeveImpedirAInicializacao() {
		assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties("curto", Duration.ofHours(2))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

}
