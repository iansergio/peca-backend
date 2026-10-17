package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Token JWT para enviar no header Authorization: Bearer <token>")
public record TokenResponse(
		@Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
		@Schema(example = "Bearer") String tipo,
		@Schema(description = "Momento em que o token expira") Instant expiraEm,
		@Schema(description = "Id do usuario autenticado") UUID usuarioId) {
}
