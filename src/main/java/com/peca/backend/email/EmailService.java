package com.peca.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de emails transacionais via SMTP (configurado em spring.mail.*).
 */
@Service
public class EmailService {

	static final String ASSUNTO_OTP = "Seu código de acesso - PEÇA";
	static final int VALIDADE_OTP_MINUTOS = 10;

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;
	private final String remetente;

	public EmailService(JavaMailSender mailSender, @Value("${app.mail.remetente}") String remetente) {
		this.mailSender = mailSender;
		this.remetente = remetente;
	}

	public void enviarCodigoOtp(String destinatario, String codigo) {
		SimpleMailMessage mensagem = new SimpleMailMessage();
		mensagem.setFrom(remetente);
		mensagem.setTo(destinatario);
		mensagem.setSubject(ASSUNTO_OTP);
		mensagem.setText("""
				Olá!

				Seu código de acesso ao PEÇA é:

				    %s

				Ele expira em %d minutos. Se você não solicitou este código, pode ignorar este email com segurança.

				Equipe PEÇA
				""".formatted(codigo, VALIDADE_OTP_MINUTOS));

		try {
			mailSender.send(mensagem);
		} catch (MailException e) {
			log.error("Falha ao enviar codigo de acesso por email", e);
			throw new EnvioEmailException("Nao foi possivel enviar o codigo por email. Tente novamente mais tarde.", e);
		}
	}

}
