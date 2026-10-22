package com.peca.backend.auth;

public class CodigoInvalidoException extends RuntimeException {

	public CodigoInvalidoException() {
		super("Codigo invalido, expirado ou ja utilizado");
	}

}
