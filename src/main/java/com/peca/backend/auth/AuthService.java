package com.peca.backend.auth;

import com.peca.backend.auth.dto.LoginRequest;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;
	// Hash comparado quando o email nao existe, para que a resposta leve o mesmo
	// tempo e nao revele quais emails estao cadastrados.
	private final String hashFicticio;

	@Autowired
	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			JwtProperties properties) {
		this(usuarioRepository, passwordEncoder, jwtEncoder, properties, Clock.systemUTC());
	}

	AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			JwtProperties properties, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
		this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		Optional<Usuario> usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase());
		String hash = usuario.map(Usuario::getSenha).orElse(hashFicticio);
		if (!passwordEncoder.matches(request.senha(), hash) || usuario.isEmpty()) {
			throw new CredenciaisInvalidasException();
		}
		return gerarToken(usuario.get());
	}

	private TokenResponse gerarToken(Usuario usuario) {
		Instant agora = clock.instant();
		Instant expiraEm = agora.plus(properties.expiracao());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("peca-backend")
				.subject(usuario.getId().toString())
				.issuedAt(agora)
				.expiresAt(expiraEm)
				.claim("nome", usuario.getNome())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", expiraEm, usuario.getId());
	}

}
