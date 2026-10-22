package com.peca.backend.peca.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Dados para criar ou atualizar um anuncio de peca")
public record PecaRequest(
		@Schema(description = "Titulo do anuncio", example = "Tela OLED iPhone 13")
		@NotBlank @Size(max = 120)
		String titulo,

		@Schema(description = "Descricao detalhada", example = "Tela original, sem riscos, com garantia de 90 dias")
		@Size(max = 2000)
		String descricao,

		@Schema(description = "Preco em reais", example = "450.00")
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2)
		BigDecimal preco,

		@Schema(description = "Categoria da peca", example = "Tela")
		@NotBlank @Size(max = 60)
		String categoria,

		@Schema(description = "Modelo de celular compativel", example = "iPhone 13")
		@NotBlank @Size(max = 80)
		String modeloCelular,

		@Schema(description = "Numero de contato (WhatsApp) do vendedor com DDD, apenas digitos. "
				+ "Obrigatorio para anunciar; fica salvo no perfil do vendedor", example = "11999998888")
		@NotBlank @Pattern(regexp = "\\d{10,13}", message = "deve conter de 10 a 13 digitos")
		String whatsapp) {
}
