package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitadorPedidosSemContaTest {

	private static final String EMAIL = "maria@example.com";

	private final RelogioAjustavel relogio = new RelogioAjustavel();
	private final LimitadorPedidosSemConta limitador = new LimitadorPedidosSemConta(relogio);

	// R-8
	@Test
	void pedidoDeveContarAteSairDaJanela() {
		limitador.registrar(EMAIL);
		relogio.avancar(Duration.ofMinutes(59));
		assertThat(limitador.pedidosRecentes(EMAIL)).hasSize(1);

		relogio.avancar(Duration.ofMinutes(1));
		assertThat(limitador.pedidosRecentes(EMAIL)).isEmpty();
	}

	// R-8
	@Test
	void emailsDevemContarSeparadamente() {
		limitador.registrar(EMAIL);

		assertThat(limitador.pedidosRecentes("outra@example.com")).isEmpty();
	}

	// R-8: o email nao fica em texto puro na memoria.
	@Test
	void chaveNaoDeveConterOEmail() {
		limitador.registrar(EMAIL);

		assertThat(limitador.chavesMonitoradas()).singleElement().asString().doesNotContain("maria", "example.com");
	}

	// R-8: o pedido sai da memoria ao deixar a janela.
	@Test
	void emailsInativosDevemSairDaMemoria() {
		limitador.registrar(EMAIL);
		relogio.avancar(Duration.ofMinutes(61));

		limitador.registrar("outra@example.com");

		assertThat(limitador.chavesMonitoradas()).hasSize(1);
	}

	private static final class RelogioAjustavel extends Clock {

		private Instant agora = Instant.parse("2026-01-01T12:00:00Z");

		void avancar(Duration duracao) {
			agora = agora.plus(duracao);
		}

		@Override
		public Instant instant() {
			return agora;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}

}
