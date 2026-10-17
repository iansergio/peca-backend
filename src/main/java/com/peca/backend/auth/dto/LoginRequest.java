package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginRequest(
		@Schema(example = "maria@email.com") @NotBlank String email,
		@Schema(example = "senhaForte123", accessMode = Schema.AccessMode.WRITE_ONLY) @NotBlank String senha) {
}
