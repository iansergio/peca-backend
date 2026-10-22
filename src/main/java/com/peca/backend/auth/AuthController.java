package com.peca.backend.auth;

import com.peca.backend.auth.dto.SolicitarCodigoRequest;
import com.peca.backend.auth.dto.SolicitarCodigoResponse;
import com.peca.backend.auth.dto.TokenResponse;
import com.peca.backend.auth.dto.VerificarCodigoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
			description = "Se o email ainda nao estiver cadastrado, o usuario e criado automaticamente. "
					+ "O codigo vale por 10 minutos e, por enquanto, e impresso no console do servidor.")
	@ApiResponse(responseCode = "200", description = "Codigo gerado e enviado")
	@ApiResponse(responseCode = "400", description = "Email invalido")
	public SolicitarCodigoResponse solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest request) {
		return service.solicitarCodigo(request);
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
