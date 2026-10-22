package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Email e codigo de 6 digitos recebido")
public record VerificarCodigoRequest(
		@Schema(example = "maria@email.com") @NotBlank @Email String email,
		@Schema(example = "123456") @NotBlank @Pattern(regexp = "\\d{6}", message = "deve conter 6 digitos") String codigo) {
}
