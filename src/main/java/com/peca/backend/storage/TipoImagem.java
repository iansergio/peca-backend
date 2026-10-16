package com.peca.backend.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagem aceitos, identificados pelos bytes iniciais do arquivo
 * (e nao pelo Content-Type enviado pelo cliente, que pode ser falsificado).
 */
enum TipoImagem {

	JPEG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp");

	private final String contentType;
	private final String extensao;

	TipoImagem(String contentType, String extensao) {
		this.contentType = contentType;
		this.extensao = extensao;
	}

	String contentType() {
		return contentType;
	}

	String extensao() {
		return extensao;
	}

	static Optional<TipoImagem> detectar(byte[] bytes) {
		return Arrays.stream(values()).filter(tipo -> tipo.corresponde(bytes)).findFirst();
	}

	private boolean corresponde(byte[] b) {
		return switch (this) {
			case JPEG -> b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
			case PNG -> b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
					&& b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
			case WEBP -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
					&& b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
		};
	}

}
