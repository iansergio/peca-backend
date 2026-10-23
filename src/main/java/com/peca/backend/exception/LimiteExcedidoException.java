package com.peca.backend.exception;

import java.time.Duration;

/**
 * Limite de requisicoes excedido; vira HTTP 429 com o header Retry-After.
 */
public class LimiteExcedidoException extends RuntimeException {

	private final long segundosParaNovaTentativa;

	public LimiteExcedidoException(Duration espera) {
		this(Math.max(1, (espera.toMillis() + 999) / 1000));
	}

	private LimiteExcedidoException(long segundos) {
		super("Muitas solicitacoes. Tente novamente em " + segundos + " segundos");
		this.segundosParaNovaTentativa = segundos;
	}

	public long getSegundosParaNovaTentativa() {
		return segundosParaNovaTentativa;
	}

}
