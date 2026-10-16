package com.peca.backend.peca.dto;

import com.peca.backend.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Dados publicos do vendedor (sem email ou senha)")
public record VendedorResponse(
		@Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
		@Schema(example = "Maria Silva") String nome,
		@Schema(example = "11999998888") String whatsapp) {

	public static VendedorResponse from(Usuario usuario) {
		return new VendedorResponse(usuario.getId(), usuario.getNome(), usuario.getWhatsapp());
	}

}
