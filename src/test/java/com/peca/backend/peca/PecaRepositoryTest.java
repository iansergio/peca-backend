package com.peca.backend.peca;

import static org.assertj.core.api.Assertions.assertThat;

import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class PecaRepositoryTest {

	@Autowired
	private PecaRepository repository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@BeforeEach
	void setUp() {
		Usuario vendedor = new Usuario();
		vendedor.setNome("Maria Silva");
		vendedor.setEmail("maria@email.com");
		vendedor.setSenha("hash");
		usuarioRepository.save(vendedor);

		repository.save(peca("Tela iPhone 13", "iPhone 13", "450.00", vendedor));
		repository.save(peca("Bateria iPhone 13 Pro", "iPhone 13 Pro", "200.00", vendedor));
		repository.save(peca("Tela Galaxy S21", "Galaxy S21", "300.00", vendedor));
	}

	private static Peca peca(String titulo, String modelo, String preco, Usuario vendedor) {
		Peca peca = new Peca();
		peca.setTitulo(titulo);
		peca.setPreco(new BigDecimal(preco));
		peca.setCategoria("Tela");
		peca.setModeloCelular(modelo);
		peca.setVendedor(vendedor);
		return peca;
	}

	private List<String> titulos(String modelo, String precoMaximo) {
		BigDecimal preco = precoMaximo == null ? null : new BigDecimal(precoMaximo);
		return repository.findAll(PecaSpecifications.filtrar(modelo, preco), Sort.by("preco")).stream()
				.map(Peca::getTitulo)
				.toList();
	}

	@Test
	void semFiltrosDeveRetornarTodasOrdenadasPorPreco() {
		assertThat(titulos(null, null))
				.containsExactly("Bateria iPhone 13 Pro", "Tela Galaxy S21", "Tela iPhone 13");
	}

	@Test
	void deveFiltrarPorModeloSemDiferenciarMaiusculas() {
		assertThat(titulos("iphone 13", null))
				.containsExactly("Bateria iPhone 13 Pro", "Tela iPhone 13");
	}

	@Test
	void deveFiltrarPorPrecoMaximoInclusivo() {
		assertThat(titulos(null, "300.00"))
				.containsExactly("Bateria iPhone 13 Pro", "Tela Galaxy S21");
	}

	@Test
	void deveCombinarModeloEPrecoMaximo() {
		assertThat(titulos("iPhone", "250")).containsExactly("Bateria iPhone 13 Pro");
	}

}
