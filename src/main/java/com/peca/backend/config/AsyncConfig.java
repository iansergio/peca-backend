package com.peca.backend.config;

import java.util.concurrent.RejectedExecutionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Fila limitada para o envio de email. Fila cheia descarta o envio em vez de rodar na thread da
 * requisicao, para a resposta nunca depender do SMTP.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	public static final String EXECUTOR_EMAIL = "executorEmail";
	static final int THREADS_EMAIL = 2;
	static final int CAPACIDADE_FILA_EMAIL = 100;

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Bean(EXECUTOR_EMAIL)
	ThreadPoolTaskExecutor executorEmail() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(THREADS_EMAIL);
		executor.setMaxPoolSize(THREADS_EMAIL);
		executor.setQueueCapacity(CAPACIDADE_FILA_EMAIL);
		executor.setThreadNamePrefix("envio-email-");
		executor.setRejectedExecutionHandler(descartarComLog());
		return executor;
	}

	private static RejectedExecutionHandler descartarComLog() {
		return (tarefa, pool) -> log.error("Fila de envio de email cheia; envio descartado");
	}

}
