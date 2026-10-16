package com.peca.backend.peca.dto;

import com.peca.backend.peca.Peca;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Anuncio de peca")
public record PecaResponse(
		@Schema(example = "7c9e6679-7425-40de-944b-e07fc1f90ae7") UUID id,
		@Schema(example = "Tela OLED iPhone 13") String titulo,
		@Schema(example = "Tela original, sem riscos, com garantia de 90 dias") String descricao,
		@Schema(example = "450.00") BigDecimal preco,
		@Schema(example = "Tela") String categoria,
		@Schema(example = "iPhone 13") String modeloCelular,
		VendedorResponse vendedor) {

	public static PecaResponse from(Peca peca) {
		return new PecaResponse(peca.getId(), peca.getTitulo(), peca.getDescricao(), peca.getPreco(),
				peca.getCategoria(), peca.getModeloCelular(), VendedorResponse.from(peca.getVendedor()));
	}

}
