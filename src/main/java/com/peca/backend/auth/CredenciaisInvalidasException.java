package com.peca.backend.auth;

public class CredenciaisInvalidasException extends RuntimeException {

	public CredenciaisInvalidasException() {
		super("Email ou senha invalidos");
	}

}
