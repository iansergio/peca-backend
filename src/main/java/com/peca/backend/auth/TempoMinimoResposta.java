package com.peca.backend.auth;

import java.time.Duration;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Faz a acao levar pelo menos {@link #MINIMO}, com sucesso ou erro, para o tempo de resposta do pedido de
 * codigo nao revelar se houve gravacao (e, portanto, se o email tem conta).
 */
@Component
public class TempoMinimoResposta {

	static final Duration MINIMO = Duration.ofMillis(50);
	private final LongSupplier relogioNanos;
	private final LongConsumer esperarNanos;

	@Autowired
	public TempoMinimoResposta() {
		this(System::nanoTime, TempoMinimoResposta::dormir);
	}

	TempoMinimoResposta(LongSupplier relogioNanos, LongConsumer esperarNanos) {
		this.relogioNanos = relogioNanos;
		this.esperarNanos = esperarNanos;
	}

	public <T> T executar(Supplier<T> acao) {
		long inicio = relogioNanos.getAsLong();
		try {
			return acao.get();
		} finally {
			long restante = MINIMO.toNanos() - (relogioNanos.getAsLong() - inicio);
			if (restante > 0) {
				esperarNanos.accept(restante);
			}
		}
	}

	private static void dormir(long nanos) {
		try {
			Thread.sleep(Duration.ofNanos(nanos));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
