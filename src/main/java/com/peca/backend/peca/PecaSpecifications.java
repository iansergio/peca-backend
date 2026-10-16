package com.peca.backend.peca;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class PecaSpecifications {

	private PecaSpecifications() {
	}

	static Specification<Peca> filtrar(String modeloCelular, BigDecimal precoMaximo) {
		List<Specification<Peca>> filtros = new ArrayList<>();
		if (modeloCelular != null && !modeloCelular.isBlank()) {
			String termo = "%" + modeloCelular.trim().toLowerCase() + "%";
			filtros.add((root, query, cb) -> cb.like(cb.lower(root.get("modeloCelular")), termo));
		}
		if (precoMaximo != null) {
			filtros.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("preco"), precoMaximo));
		}
		return Specification.allOf(filtros);
	}

}
