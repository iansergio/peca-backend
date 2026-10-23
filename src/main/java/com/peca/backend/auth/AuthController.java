package com.peca.backend.auth;

import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.auth.dto.SolicitarCodigoResponse;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.auth.dto.VerificarCodigoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Login sem senha por codigo enviado ao email (OTP) e emissao de token JWT")
public class AuthController {

	private final AuthService service;

	public AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/solicitar-codigo")
	@Operation(summary = "Envia um codigo de 6 digitos para o email",
			description = "Nome e email sao obrigatorios. Se o email ainda nao estiver cadastrado, o usuario so e criado "
					+ "quando o codigo for validado, com o nome informado (o nome de um usuario existente nao e alterado). "
					+ "O codigo vale por 10 minutos e e enviado por email. Limites: 1 pedido a cada 60 segundos e "
					+ "5 por hora para o mesmo email, e 20 por hora por IP.")
	@ApiResponse(responseCode = "200", description = "Codigo gerado e enviado")
	@ApiResponse(responseCode = "400", description = "Nome ausente ou email invalido")
	@ApiResponse(responseCode = "429", description = "Limite de solicitacoes excedido; aguarde o tempo do header Retry-After",
			headers = @Header(name = HttpHeaders.RETRY_AFTER, description = "Segundos ate poder solicitar de novo",
					schema = @Schema(type = "integer")))
	@ApiResponse(responseCode = "502", description = "Falha ao enviar o email com o codigo")
	public SolicitarCodigoResponse solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest request,
			HttpServletRequest httpRequest) {
		// Atras de proxy, o IP real vem do X-Forwarded-For (server.forward-headers-strategy=framework).
		return service.solicitarCodigo(request, httpRequest.getRemoteAddr());
	}

	@PostMapping("/verificar-codigo")
	@Operation(summary = "Valida o codigo e retorna um token JWT com o perfil do usuario",
			description = "Copie o campo token e use o botao Authorize do Swagger para chamar os endpoints protegidos.")
	@ApiResponse(responseCode = "200", description = "Codigo valido, token emitido")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "401", description = "Codigo invalido, expirado ou ja utilizado")
	public TokenResponse verificarCodigo(@Valid @RequestBody VerificarCodigoRequest request) {
		return service.verificarCodigo(request);
	}

}
