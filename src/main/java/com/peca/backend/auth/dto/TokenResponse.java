package com.peca.backend.auth.dto;

import com.peca.backend.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Token JWT para enviar no header Authorization: Bearer <token>")
public record TokenResponse(
		@Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
		@Schema(example = "Bearer") String tipo,
		@Schema(description = "Momento em que o token expira") Instant expiraEm,
		@Schema(description = "Perfil resumido do usuario autenticado") UsuarioResponse usuario) {
}
