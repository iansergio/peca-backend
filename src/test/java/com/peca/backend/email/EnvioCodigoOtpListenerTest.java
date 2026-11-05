package com.peca.backend.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvioCodigoOtpListenerTest {

	private static final CodigoOtpSolicitado EVENTO = new CodigoOtpSolicitado("maria@example.com", "123456");

	@Mock
	private EmailService emailService;

	@InjectMocks
	private EnvioCodigoOtpListener listener;

	// R-11
	@Test
	void deveEnviarOCodigoDoEvento() {
		listener.enviar(EVENTO);

		verify(emailService).enviarCodigoOtp("maria@example.com", "123456");
	}

	// R-11, AC-11
	@Test
	void falhaNoEnvioNaoDevePropagar() {
		doThrow(new EnvioEmailException("falhou", null)).when(emailService)
				.enviarCodigoOtp("maria@example.com", "123456");

		assertThatCode(() -> listener.enviar(EVENTO)).doesNotThrowAnyException();
	}

	// Dado sensivel nunca aparece em log.
	@Test
	void toStringNaoDeveExporEmailNemCodigo() {
		assertThat(EVENTO.toString()).doesNotContain("maria", "123456");
	}

}
