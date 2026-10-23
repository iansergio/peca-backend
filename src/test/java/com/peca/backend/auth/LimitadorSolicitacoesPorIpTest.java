package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peca.backend.exception.LimiteExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitadorSolicitacoesPorIpTest {

	private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-01-01T12:00:00Z"));
	private final LimitadorSolicitacoesPorIp limitador =
			new LimitadorSolicitacoesPorIp(new OtpProperties(Duration.ofSeconds(60), 5, 3), relogio);

	@Test
	void deveRecusarAposOLimiteInformandoQuandoLibera() {
		limitador.registrar("203.0.113.1");
		relogio.avancar(Duration.ofMinutes(10));
		limitador.registrar("203.0.113.1");
		limitador.registrar("203.0.113.1");

		// O primeiro pedido sai da janela em 50 minutos.
		assertThatThrownBy(() -> limitador.registrar("203.0.113.1"))
				.isInstanceOf(LimiteExcedidoException.class)
				.extracting("segundosParaNovaTentativa").isEqualTo(3000L);
	}

	@Test
	void janelaDeveDeslizarConformeOsPedidosAntigosExpiram() {
		limitador.registrar("203.0.113.1");
		relogio.avancar(Duration.ofMinutes(30));
		limitador.registrar("203.0.113.1");
		limitador.registrar("203.0.113.1");

		relogio.avancar(Duration.ofMinutes(30));
		assertThatCode(() -> limitador.registrar("203.0.113.1")).doesNotThrowAnyException();
		assertThatThrownBy(() -> limitador.registrar("203.0.113.1")).isInstanceOf(LimiteExcedidoException.class);
	}

	@Test
	void pedidoRecusadoNaoDeveContar() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar("203.0.113.1");
		}
		for (int i = 0; i < 10; i++) {
			assertThatThrownBy(() -> limitador.registrar("203.0.113.1")).isInstanceOf(LimiteExcedidoException.class);
		}

		relogio.avancar(Duration.ofHours(1));
		assertThatCode(() -> limitador.registrar("203.0.113.1")).doesNotThrowAnyException();
	}

	@Test
	void ipsDevemTerLimitesIndependentes() {
		for (int i = 0; i < 3; i++) {
			limitador.registrar("203.0.113.1");
		}

		assertThatCode(() -> limitador.registrar("203.0.113.2")).doesNotThrowAnyException();
	}

	@Test
	void ipsInativosDevemSairDaMemoria() {
		limitador.registrar("203.0.113.1");
		limitador.registrar("203.0.113.2");

		relogio.avancar(Duration.ofHours(1).plusMinutes(1));
		limitador.registrar("203.0.113.3");

		assertThat(limitador.ipsMonitorados()).isEqualTo(1);
	}

	private static final class RelogioAjustavel extends Clock {

		private Instant agora;

		RelogioAjustavel(Instant agora) {
			this.agora = agora;
		}

		void avancar(Duration duracao) {
			agora = agora.plus(duracao);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return agora;
		}

	}

}
