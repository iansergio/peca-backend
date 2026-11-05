package com.peca.backend.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

	// R-11: fila cheia nao falha nem roda na thread da requisicao.
	@Test
	void filaCheiaDeveDescartarOEnvioSemLancarErro() {
		ThreadPoolTaskExecutor executor = new AsyncConfig().executorEmail();
		executor.initialize();
		CountDownLatch liberar = new CountDownLatch(1);
		Runnable travada = () -> {
			try {
				liberar.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		try {
			for (int i = 0; i < AsyncConfig.THREADS_EMAIL + AsyncConfig.CAPACIDADE_FILA_EMAIL; i++) {
				executor.execute(travada);
			}

			assertThatCode(() -> executor.execute(travada)).doesNotThrowAnyException();
		} finally {
			liberar.countDown();
			executor.shutdown();
		}
	}

}
