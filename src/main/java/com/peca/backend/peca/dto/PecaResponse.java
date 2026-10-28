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
		@Schema(description = "URL publica da imagem, servida pelo front-end (nula se nao houver)",
				example = "/imagens/7c9e6679-7425-40de-944b-e07fc1f90ae7/0b3f5c1e-8d2a-4f6b-9c7e-1a2b3c4d5e6f.jpg")
		String imagemUrl,
		VendedorResponse vendedor) {

	public static PecaResponse from(Peca peca) {
		return new PecaResponse(peca.getId(), peca.getTitulo(), peca.getDescricao(), peca.getPreco(),
				peca.getCategoria(), peca.getModeloCelular(), peca.getImagemUrl(), VendedorResponse.from(peca.getVendedor()));
	}

}
