package com.peca.backend.auth;

import com.peca.backend.exception.LimiteExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limite de solicitacoes de codigo por IP em janela deslizante de 1 hora, em memoria.
 * Vale por instancia da aplicacao e e zerado ao reiniciar.
 */
@Component
public class LimitadorSolicitacoesPorIp {

	private static final Duration INTERVALO_LIMPEZA = Duration.ofMinutes(1);
	private final Map<String, Deque<Instant>> pedidos = new ConcurrentHashMap<>();
	private final int maxPorJanela;
	private final Clock clock;
	private volatile Instant ultimaLimpeza;

	@Autowired
	public LimitadorSolicitacoesPorIp(OtpProperties properties) {
		this(properties, Clock.systemUTC());
	}

	LimitadorSolicitacoesPorIp(OtpProperties properties, Clock clock) {
		this.maxPorJanela = properties.maxPedidosPorHoraIp();
		this.clock = clock;
		this.ultimaLimpeza = clock.instant();
	}

	/**
	 * Registra um pedido do IP ou lanca {@link LimiteExcedidoException} se o limite foi atingido
	 * (pedidos recusados nao contam).
	 */
	public void registrar(String ip) {
		Instant agora = clock.instant();
		Instant inicio = agora.minus(OtpProperties.JANELA);
		removerInativos(agora, inicio);

		Duration[] espera = new Duration[1];
		pedidos.compute(ip, (chave, fila) -> {
			Deque<Instant> instantes = fila == null ? new ArrayDeque<>() : fila;
			descartarAntigos(instantes, inicio);
			if (instantes.size() >= maxPorJanela) {
				espera[0] = Duration.between(agora, instantes.peekFirst().plus(OtpProperties.JANELA));
			} else {
				instantes.addLast(agora);
			}
			return instantes;
		});
		if (espera[0] != null) {
			throw new LimiteExcedidoException(espera[0]);
		}
	}

	// Evita que IPs que nao voltam mais fiquem para sempre no mapa.
	private void removerInativos(Instant agora, Instant inicio) {
		if (agora.isBefore(ultimaLimpeza.plus(INTERVALO_LIMPEZA))) {
			return;
		}
		ultimaLimpeza = agora;
		for (String ip : pedidos.keySet()) {
			pedidos.computeIfPresent(ip, (chave, instantes) -> {
				descartarAntigos(instantes, inicio);
				return instantes.isEmpty() ? null : instantes;
			});
		}
	}

	int ipsMonitorados() {
		return pedidos.size();
	}

	private static void descartarAntigos(Deque<Instant> instantes, Instant inicio) {
		while (!instantes.isEmpty() && !instantes.peekFirst().isAfter(inicio)) {
			instantes.pollFirst();
		}
	}

}
