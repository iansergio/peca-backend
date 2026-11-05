package com.peca.backend.email;

/**
 * Pedido de envio do codigo de acesso, publicado dentro da transacao que grava o codigo.
 */
public record CodigoOtpSolicitado(String destinatario, String codigo) {

	// O destinatario e o codigo nunca podem aparecer em log.
	@Override
	public String toString() {
		return "CodigoOtpSolicitado";
	}

}
