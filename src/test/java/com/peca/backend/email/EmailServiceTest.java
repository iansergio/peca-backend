package com.peca.backend.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	private EmailService service;

	@BeforeEach
	void setUp() {
		service = new EmailService(mailSender, "nao-responda@peca.test");
	}

	@Test
	void enviarCodigoOtpDeveMontarEmailComCodigoEValidade() {
		service.enviarCodigoOtp("maria@email.com", "123456");

		ArgumentCaptor<SimpleMailMessage> mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mensagem.capture());
		assertThat(mensagem.getValue().getFrom()).isEqualTo("nao-responda@peca.test");
		assertThat(mensagem.getValue().getTo()).containsExactly("maria@email.com");
		assertThat(mensagem.getValue().getSubject()).isEqualTo("Seu código de acesso - PEÇA");
		assertThat(mensagem.getValue().getText()).contains("123456").contains("10 minutos");
	}

	@Test
	void falhaNoSmtpDeveVirarEnvioEmailException() {
		doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> service.enviarCodigoOtp("maria@email.com", "123456"))
				.isInstanceOf(EnvioEmailException.class)
				.hasCauseInstanceOf(MailSendException.class);
	}

}
