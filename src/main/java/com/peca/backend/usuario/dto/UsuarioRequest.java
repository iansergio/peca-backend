package com.peca.backend.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou atualizar um usuario")
public record UsuarioRequest(
		@Schema(description = "Nome completo", example = "Maria Silva")
		@NotBlank @Size(max = 120)
		String nome,

		@Schema(description = "Email unico do usuario", example = "maria@email.com")
		@NotBlank @Email @Size(max = 160)
		String email,

		@Schema(description = "WhatsApp com DDD, apenas digitos", example = "11999998888")
		@Pattern(regexp = "\\d{10,13}", message = "deve conter de 10 a 13 digitos")
		String whatsapp) {
}
