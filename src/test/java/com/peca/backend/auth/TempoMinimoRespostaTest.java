package com.peca.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TempoMinimoRespostaTest {

	private static final long MINIMO_NANOS = TempoMinimoResposta.MINIMO.toNanos();

	private long agora;
	private final List<Long> esperas = new ArrayList<>();
	private final TempoMinimoResposta tempoMinimo = new TempoMinimoResposta(() -> agora, esperas::add);

	// R-3, AC-20
	@Test
	void acaoRapidaDeveEsperarORestante() {
		String resultado = tempoMinimo.executar(() -> {
			agora += 10_000_000;
			return "ok";
		});

		assertThat(resultado).isEqualTo("ok");
		assertThat(esperas).containsExactly(MINIMO_NANOS - 10_000_000);
	}

	// R-3: um pedido mais lento que o minimo nao ganha espera.
	@Test
	void acaoLentaNaoDeveEsperar() {
		tempoMinimo.executar(() -> {
			agora += MINIMO_NANOS + 1;
			return "ok";
		});

		assertThat(esperas).isEmpty();
	}

	// R-3, AC-20: o 429 tambem respeita o minimo.
	@Test
	void acaoComErroDeveEsperarEPropagar() {
		assertThatThrownBy(() -> tempoMinimo.executar(() -> {
			throw new IllegalStateException("limite");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(esperas).containsExactly(MINIMO_NANOS);
	}

}
