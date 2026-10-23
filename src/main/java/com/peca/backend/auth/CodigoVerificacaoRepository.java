package com.peca.backend.auth;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, UUID> {

	List<CodigoVerificacao> findByEmailAndUtilizadoFalse(String email);

	// Solicitacoes recentes do email, da mais antiga para a mais nova (base do cooldown e do limite por hora).
	List<CodigoVerificacao> findByEmailAndCriadoEmAfterOrderByCriadoEmAsc(String email, LocalDateTime inicio);

	// Lock pessimista: duas verificacoes simultaneas nao conseguem usar o mesmo codigo.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<CodigoVerificacao> findFirstByEmailAndUtilizadoFalseOrderByExpiraEmDesc(String email);

}
