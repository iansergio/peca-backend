package com.peca.backend.auth;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrai o id do usuario logado do token (claim {@code sub}).
 */
public final class UsuarioAutenticado {

	private UsuarioAutenticado() {
	}

	public static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
