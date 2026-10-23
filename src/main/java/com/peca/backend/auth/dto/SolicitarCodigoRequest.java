package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nome e email que recebera o codigo de acesso")
public record SolicitarCodigoRequest(
		@Schema(description = "Nome completo (usado no cadastro de um usuario novo)", example = "Maria Silva")
		@NotBlank @Size(max = 120)
		String nome,

		@Schema(example = "maria@email.com") @NotBlank @Email @Size(max = 160) String email) {
}
