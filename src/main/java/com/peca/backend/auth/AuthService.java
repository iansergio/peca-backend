package com.peca.backend.auth;

import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.auth.dto.SolicitarCodigoResponse;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.auth.dto.VerificarCodigoRequest;
import com.peca.backend.email.EmailService;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import com.peca.backend.usuario.UsuarioService;
import com.peca.backend.usuario.dto.UsuarioResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	static final Duration VALIDADE_CODIGO = Duration.ofMinutes(10);
	static final int MAX_TENTATIVAS = 5;
	private final UsuarioRepository usuarioRepository;
	private final CodigoVerificacaoRepository codigoRepository;
	private final JwtEncoder jwtEncoder;
	private final EmailService emailService;
	private final JwtProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	@Autowired
	public AuthService(UsuarioRepository usuarioRepository, CodigoVerificacaoRepository codigoRepository,
			JwtEncoder jwtEncoder, EmailService emailService, JwtProperties properties) {
		this(usuarioRepository, codigoRepository, jwtEncoder, emailService, properties, Clock.systemUTC());
	}

	AuthService(UsuarioRepository usuarioRepository, CodigoVerificacaoRepository codigoRepository,
			JwtEncoder jwtEncoder, EmailService emailService, JwtProperties properties, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.codigoRepository = codigoRepository;
		this.jwtEncoder = jwtEncoder;
		this.emailService = emailService;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public SolicitarCodigoResponse solicitarCodigo(SolicitarCodigoRequest request) {
		String email = UsuarioService.normalizarEmail(request.email());
		Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> cadastrar(request.nome(), email));

		// Apenas o codigo mais recente fica valido.
		codigoRepository.findByEmailAndUtilizadoFalse(email).forEach(antigo -> antigo.setUtilizado(true));

		CodigoVerificacao codigo = new CodigoVerificacao();
		codigo.setUsuario(usuario);
		codigo.setEmail(email);
		codigo.setCodigo(gerarCodigo());
		codigo.setExpiraEm(LocalDateTime.now(clock).plus(VALIDADE_CODIGO));
		codigoRepository.save(codigo);

		// Dentro da transacao: se o envio falhar, o codigo (e o cadastro novo) sao desfeitos.
		emailService.enviarCodigoOtp(email, codigo.getCodigo());
		return new SolicitarCodigoResponse("Codigo enviado para o email informado", codigo.getExpiraEm());
	}

	// noRollbackFor: o incremento de tentativas precisa ser gravado mesmo quando o codigo esta errado.
	@Transactional(noRollbackFor = CodigoInvalidoException.class)
	public TokenResponse verificarCodigo(VerificarCodigoRequest request) {
		String email = UsuarioService.normalizarEmail(request.email());
		CodigoVerificacao codigo = codigoRepository.findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc(email)
				.orElseThrow(CodigoInvalidoException::new);

		if (!LocalDateTime.now(clock).isBefore(codigo.getExpiraEm())) {
			codigo.setUtilizado(true);
			throw new CodigoInvalidoException();
		}
		if (!MessageDigest.isEqual(codigo.getCodigo().getBytes(StandardCharsets.UTF_8),
				request.codigo().getBytes(StandardCharsets.UTF_8))) {
			codigo.setTentativas(codigo.getTentativas() + 1);
			if (codigo.getTentativas() >= MAX_TENTATIVAS) {
				codigo.setUtilizado(true);
			}
			throw new CodigoInvalidoException();
		}

		codigo.setUtilizado(true);
		return gerarToken(codigo.getUsuario());
	}

	// O nome so e usado no cadastro: um usuario existente nao tem o nome sobrescrito por quem pede o codigo,
	// ja que o email ainda nao foi confirmado nesse ponto. Para alterar, use PUT /api/usuarios/{id}.
	private Usuario cadastrar(String nome, String email) {
		Usuario usuario = new Usuario();
		usuario.setEmail(email);
		usuario.setNome(nome.trim());
		return usuarioRepository.save(usuario);
	}

	private String gerarCodigo() {
		return "%06d".formatted(random.nextInt(1_000_000));
	}

	private TokenResponse gerarToken(Usuario usuario) {
		Instant agora = clock.instant();
		Instant expiraEm = agora.plus(properties.expiracao());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("peca-backend")
				.subject(usuario.getId().toString())
				.issuedAt(agora)
				.expiresAt(expiraEm)
				.claim("email", usuario.getEmail())
				.claim("nome", usuario.getNome())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", expiraEm, UsuarioResponse.from(usuario));
	}

}
