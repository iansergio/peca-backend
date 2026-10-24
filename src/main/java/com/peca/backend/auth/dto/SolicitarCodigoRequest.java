package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Email que recebera o codigo de acesso e, no cadastro, o nome")
public record SolicitarCodigoRequest(
		@Schema(description = "Nome completo. Obrigatorio so no cadastro (email sem conta); no login pode ser omitido",
				example = "Maria Silva", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
		@Size(max = 120)
		String nome,

		@Schema(example = "maria@email.com") @NotBlank @Email @Size(max = 160) String email) {
}
