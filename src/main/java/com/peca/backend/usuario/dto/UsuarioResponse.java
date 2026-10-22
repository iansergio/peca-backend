package com.peca.backend.usuario.dto;

import com.peca.backend.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Dados publicos de um usuario")
public record UsuarioResponse(
		@Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
		@Schema(example = "Maria Silva") String nome,
		@Schema(example = "maria@email.com") String email,
		@Schema(example = "11999998888") String whatsapp) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getWhatsapp());
	}

}
