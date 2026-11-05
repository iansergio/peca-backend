package com.peca.backend.email;

import com.peca.backend.config.AsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia o codigo depois do commit e fora da requisicao: a resposta do pedido nao depende do SMTP
 * (nem revela, pelo tempo ou por erro, se o email tem conta), e transacao desfeita nao envia nada.
 */
@Component
public class EnvioCodigoOtpListener {

	private final EmailService emailService;

	public EnvioCodigoOtpListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@Async(AsyncConfig.EXECUTOR_EMAIL)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void enviar(CodigoOtpSolicitado evento) {
		try {
			emailService.enviarCodigoOtp(evento.destinatario(), evento.codigo());
		} catch (EnvioEmailException e) {
			// O EmailService ja registrou a falha; a pessoa pede outro codigo pelo reenvio.
		}
	}

}
