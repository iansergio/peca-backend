package com.peca.backend.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de solicitacao de codigo OTP (protecao contra envio de email em massa e forca bruta).
 *
 * @param intervaloEntrePedidos tempo minimo entre dois pedidos para o mesmo email (ate 1 hora)
 * @param maxPedidosPorHoraEmail pedidos por email na ultima hora
 * @param maxPedidosPorHoraIp pedidos por IP na ultima hora
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(Duration intervaloEntrePedidos, int maxPedidosPorHoraEmail, int maxPedidosPorHoraIp) {

	public static final Duration JANELA = Duration.ofHours(1);

	public OtpProperties {
		if (intervaloEntrePedidos == null || intervaloEntrePedidos.isNegative()) {
			intervaloEntrePedidos = Duration.ofSeconds(60);
		}
		if (intervaloEntrePedidos.compareTo(JANELA) > 0) {
			throw new IllegalStateException("app.otp.intervalo-entre-pedidos nao pode passar de 1 hora");
		}
		if (maxPedidosPorHoraEmail <= 0) {
			maxPedidosPorHoraEmail = 5;
		}
		if (maxPedidosPorHoraIp <= 0) {
			maxPedidosPorHoraIp = 20;
		}
	}

}
