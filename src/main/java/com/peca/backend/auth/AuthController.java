package com.peca.backend.auth;

import com.peca.backend.auth.dto.LoginRequest;
import com.peca.backend.auth.dto.TokenResponse;
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
@Tag(name = "Autenticacao", description = "Login e emissao de token JWT")
public class AuthController {

	private final AuthService service;

	public AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/login")
	@Operation(summary = "Autentica com email e senha e retorna um token JWT",
			description = "Copie o campo token e use o botao Authorize do Swagger para chamar os endpoints protegidos.")
	@ApiResponse(responseCode = "200", description = "Login realizado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "401", description = "Email ou senha invalidos")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}

}
