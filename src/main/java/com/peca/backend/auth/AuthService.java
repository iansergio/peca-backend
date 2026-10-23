package com.peca.backend.auth;

import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.auth.dto.SolicitarCodigoResponse;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.auth.dto.VerificarCodigoRequest;
import com.peca.backend.email.EmailService;
import com.peca.backend.exception.LimiteExcedidoException;
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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
	private final UsuarioService usuarioService;
	private final CodigoVerificacaoRepository codigoRepository;
	private final JwtEncoder jwtEncoder;
	private final EmailService emailService;
	private final JwtProperties properties;
	private final OtpProperties otpProperties;
	private final LimitadorSolicitacoesPorIp limitadorPorIp;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	@Autowired
	public AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			CodigoVerificacaoRepository codigoRepository, JwtEncoder jwtEncoder, EmailService emailService,
			JwtProperties properties, OtpProperties otpProperties, LimitadorSolicitacoesPorIp limitadorPorIp) {
		this(usuarioRepository, usuarioService, codigoRepository, jwtEncoder, emailService, properties, otpProperties,
				limitadorPorIp, Clock.systemUTC());
	}

	AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			CodigoVerificacaoRepository codigoRepository, JwtEncoder jwtEncoder, EmailService emailService,
			JwtProperties properties, OtpProperties otpProperties, LimitadorSolicitacoesPorIp limitadorPorIp,
			Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.codigoRepository = codigoRepository;
		this.jwtEncoder = jwtEncoder;
		this.emailService = emailService;
		this.properties = properties;
		this.otpProperties = otpProperties;
		this.limitadorPorIp = limitadorPorIp;
		this.clock = clock;
	}

	@Transactional
	public SolicitarCodigoResponse solicitarCodigo(SolicitarCodigoRequest request, String ip) {
		String email = UsuarioService.normalizarEmail(request.email());
		LocalDateTime agora = LocalDateTime.now(clock);

		// Os limites sao checados antes de gerar o codigo e enviar o email, e valem igualmente para
		// emails cadastrados ou nao (a resposta nao revela se o email existe).
		limitadorPorIp.registrar(ip);
		exigirDentroDoLimite(email, agora);

		// Email sem conta nao gera cadastro aqui: o nome fica guardado no codigo ate a verificacao.
		Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

		// Apenas o codigo mais recente fica valido.
		codigoRepository.findByEmailAndUtilizadoFalse(email).forEach(antigo -> antigo.setUtilizado(true));

		CodigoVerificacao codigo = new CodigoVerificacao();
		codigo.setUsuario(usuario);
		if (usuario == null) {
			codigo.setNome(request.nome().trim());
		}
		codigo.setEmail(email);
		codigo.setCodigo(gerarCodigo());
		codigo.setCriadoEm(agora);
		codigo.setExpiraEm(agora.plus(VALIDADE_CODIGO));
		codigoRepository.save(codigo);

		// Dentro da transacao: se o envio falhar, o codigo e desfeito.
		emailService.enviarCodigoOtp(email, codigo.getCodigo());
		return new SolicitarCodigoResponse("Codigo enviado para o email informado", codigo.getExpiraEm());
	}

	// Cooldown entre pedidos e limite por hora do mesmo email. Com 5 codigos/hora e 5 tentativas
	// por codigo, um email aceita no maximo 25 chutes por hora.
	private void exigirDentroDoLimite(String email, LocalDateTime agora) {
		List<CodigoVerificacao> recentes = codigoRepository
				.findByEmailAndCriadoEmAfterOrderByCriadoEmAsc(email, agora.minus(OtpProperties.JANELA));
		if (recentes.isEmpty()) {
			return;
		}
		LocalDateTime liberaPorIntervalo = recentes.getLast().getCriadoEm().plus(otpProperties.intervaloEntrePedidos());
		if (agora.isBefore(liberaPorIntervalo)) {
			throw new LimiteExcedidoException(Duration.between(agora, liberaPorIntervalo));
		}
		int excedentes = recentes.size() - otpProperties.maxPedidosPorHoraEmail();
		if (excedentes >= 0) {
			// Libera quando o pedido que estourou o limite sair da janela.
			LocalDateTime liberaPorHora = recentes.get(excedentes).getCriadoEm().plus(OtpProperties.JANELA);
			throw new LimiteExcedidoException(Duration.between(agora, liberaPorHora));
		}
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
		if (codigo.getUsuario() == null) {
			codigo.setUsuario(obterOuCadastrar(codigo.getNome(), email));
		}
		return gerarToken(codigo.getUsuario());
	}

	// O cadastro so acontece com o email confirmado. O nome guardado no codigo nao sobrescreve o de um
	// usuario que ja exista (ex: criado por outra verificacao). Para alterar, use PUT /api/usuarios/{id}.
	private Usuario obterOuCadastrar(String nome, String email) {
		return usuarioRepository.findByEmail(email).orElseGet(() -> {
			try {
				return usuarioService.cadastrar(nome, email);
			} catch (DataIntegrityViolationException e) {
				// Corrida: outra verificacao cadastrou o mesmo email entre a busca e o insert.
				return usuarioRepository.findByEmail(email).orElseThrow(() -> e);
			}
		});
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
