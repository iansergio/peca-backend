package com.peca.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Pedidos de codigo de emails sem conta na ultima hora, em memoria. Esses pedidos nao gravam codigo,
 * mas precisam contar no limite por email para o 429 nao revelar quais emails tem conta.
 * A chave e um digest com salt gerado na inicializacao, para nao guardar o email em texto puro.
 * Vale por instancia da aplicacao e e zerado ao reiniciar.
 */
@Component
public class LimitadorPedidosSemConta {

	private static final Duration INTERVALO_LIMPEZA = Duration.ofMinutes(1);
	private static final int TAMANHO_SALT = 32;
	private final Map<String, Deque<LocalDateTime>> pedidos = new ConcurrentHashMap<>();
	private final byte[] salt = new byte[TAMANHO_SALT];
	private final Clock clock;
	private volatile LocalDateTime ultimaLimpeza;

	@Autowired
	public LimitadorPedidosSemConta() {
		this(Clock.systemUTC());
	}

	LimitadorPedidosSemConta(Clock clock) {
		this.clock = clock;
		this.ultimaLimpeza = LocalDateTime.now(clock);
		new SecureRandom().nextBytes(salt);
	}

	public void registrar(String email) {
		LocalDateTime agora = LocalDateTime.now(clock);
		LocalDateTime inicio = agora.minus(OtpProperties.JANELA);
		removerInativos(agora, inicio);
		pedidos.compute(chave(email), (chave, fila) -> {
			Deque<LocalDateTime> instantes = fila == null ? new ArrayDeque<>() : fila;
			descartarAntigos(instantes, inicio);
			instantes.addLast(agora);
			return instantes;
		});
	}

	/** Instantes dos pedidos do email ainda dentro da janela. */
	public List<LocalDateTime> pedidosRecentes(String email) {
		LocalDateTime inicio = LocalDateTime.now(clock).minus(OtpProperties.JANELA);
		Deque<LocalDateTime> instantes = pedidos.computeIfPresent(chave(email), (chave, fila) -> {
			descartarAntigos(fila, inicio);
			return fila.isEmpty() ? null : fila;
		});
		return instantes == null ? List.of() : List.copyOf(instantes);
	}

	// Evita que emails que nao voltam mais fiquem para sempre no mapa.
	private void removerInativos(LocalDateTime agora, LocalDateTime inicio) {
		if (agora.isBefore(ultimaLimpeza.plus(INTERVALO_LIMPEZA))) {
			return;
		}
		ultimaLimpeza = agora;
		for (String chave : pedidos.keySet()) {
			pedidos.computeIfPresent(chave, (atual, instantes) -> {
				descartarAntigos(instantes, inicio);
				return instantes.isEmpty() ? null : instantes;
			});
		}
	}

	private String chave(String email) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(salt);
			return HexFormat.of().formatHex(digest.digest(email.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponivel", e);
		}
	}

	Set<String> chavesMonitoradas() {
		return Set.copyOf(pedidos.keySet());
	}

	private static void descartarAntigos(Deque<LocalDateTime> instantes, LocalDateTime inicio) {
		while (!instantes.isEmpty() && !instantes.peekFirst().isAfter(inicio)) {
			instantes.pollFirst();
		}
	}

}
