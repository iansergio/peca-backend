package com.peca.backend.usuario;

import com.peca.backend.auth.UsuarioAutenticado;
import com.peca.backend.config.OpenApiConfig;
import com.peca.backend.usuario.dto.UsuarioRequest;
import com.peca.backend.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Perfil de usuarios (o cadastro e feito em POST /api/auth/solicitar-codigo)")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	// Mapping literal: o Spring prioriza /me sobre o padrao /{id}.
	@GetMapping("/me")
	@Operation(summary = "Retorna o perfil do usuario logado",
			description = "Usa o usuario do token. Dados publicos de outros vendedores saem apenas em PecaResponse.vendedor (sem email).")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "200", description = "Perfil do usuario logado")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "404", description = "Usuario do token nao existe mais")
	public UsuarioResponse buscarAtual(@AuthenticationPrincipal Jwt jwt) {
		return service.buscarAtual(UsuarioAutenticado.id(jwt));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca a propria conta pelo id",
			description = "So o proprio usuario pode ler o seu perfil; prefira GET /api/usuarios/me.")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "200", description = "Usuario encontrado")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Tentativa de ler outra conta")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	public UsuarioResponse buscarPorId(@Parameter(description = "Id do usuario") @PathVariable UUID id,
			@AuthenticationPrincipal Jwt jwt) {
		return service.buscarPorId(id, UsuarioAutenticado.id(jwt));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza a propria conta",
			description = "Altera nome e WhatsApp. O email nao pode ser alterado (e a identidade confirmada por OTP).")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "200", description = "Usuario atualizado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Tentativa de alterar outra conta")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	public UsuarioResponse atualizar(@Parameter(description = "Id do usuario") @PathVariable UUID id,
			@Valid @RequestBody UsuarioRequest request, @AuthenticationPrincipal Jwt jwt) {
		return service.atualizar(id, request, UsuarioAutenticado.id(jwt));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove a propria conta")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "204", description = "Usuario removido")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Tentativa de remover outra conta")
	@ApiResponse(responseCode = "409", description = "Usuario possui anuncios vinculados")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	public ResponseEntity<Void> deletar(@Parameter(description = "Id do usuario") @PathVariable UUID id,
			@AuthenticationPrincipal Jwt jwt) {
		service.deletar(id, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}

}
