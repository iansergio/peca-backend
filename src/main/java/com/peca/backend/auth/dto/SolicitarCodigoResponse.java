package com.peca.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Confirmacao de envio do codigo")
public record SolicitarCodigoResponse(
		@Schema(example = "Codigo enviado para o email informado") String mensagem,
		@Schema(description = "Momento (UTC) em que o codigo expira") LocalDateTime expiraEm) {
}
