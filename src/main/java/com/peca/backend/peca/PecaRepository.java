package com.peca.backend.peca;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PecaRepository extends JpaRepository<Peca, UUID>, JpaSpecificationExecutor<Peca> {

	// Carrega o vendedor junto para evitar N+1 ao montar a resposta da listagem.
	@Override
	@EntityGraph(attributePaths = "vendedor")
	List<Peca> findAll(Specification<Peca> spec, Sort sort);

}
