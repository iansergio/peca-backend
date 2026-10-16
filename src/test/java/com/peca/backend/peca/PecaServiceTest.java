package com.peca.backend.peca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

	@Mock
	private PecaRepository repository;

	@Mock
	private UsuarioRepository usuarioRepository;

	private PecaService service;

	@BeforeEach
	void setUp() {
		service = new PecaService(repository, usuarioRepository);
	}

	private static Usuario vendedor(UUID id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Maria Silva");
		usuario.setEmail("maria@email.com");
		usuario.setSenha("hash");
		usuario.setWhatsapp("11999998888");
		return usuario;
	}

	private static PecaRequest request(UUID vendedorId) {
		return new PecaRequest(" Tela iPhone 13 ", "Original", new BigDecimal("450.00"), "Tela", "iPhone 13", vendedorId);
	}

	private static Peca pecaExistente(UUID id, Usuario vendedor) {
		Peca peca = new Peca();
		peca.setId(id);
		peca.setTitulo("Antigo");
		peca.setPreco(BigDecimal.TEN);
		peca.setCategoria("Tela");
		peca.setModeloCelular("iPhone 12");
		peca.setVendedor(vendedor);
		return peca;
	}

	@Test
	void criarDeveVincularVendedorERetornarSeusDadosPublicos() {
		UUID vendedorId = UUID.randomUUID();
		when(usuarioRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor(vendedorId)));
		when(repository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

		PecaResponse response = service.criar(request(vendedorId));

		assertThat(response.titulo()).isEqualTo("Tela iPhone 13");
		assertThat(response.vendedor().id()).isEqualTo(vendedorId);
		assertThat(response.vendedor().nome()).isEqualTo("Maria Silva");
		assertThat(response.vendedor().whatsapp()).isEqualTo("11999998888");
	}

	@Test
	void criarDeveFalharQuandoVendedorNaoExiste() {
		UUID vendedorId = UUID.randomUUID();
		when(usuarioRepository.findById(vendedorId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.criar(request(vendedorId)))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void atualizarDeveAlterarCamposQuandoVendedorEhODono() {
		UUID id = UUID.randomUUID();
		UUID vendedorId = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(vendedorId))));
		when(repository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

		PecaResponse response = service.atualizar(id, request(vendedorId));

		assertThat(response.modeloCelular()).isEqualTo("iPhone 13");
		assertThat(response.preco()).isEqualByComparingTo("450.00");
	}

	@Test
	void atualizarDeveFalharQuandoVendedorEhOutro() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(UUID.randomUUID()))));

		assertThatThrownBy(() -> service.atualizar(id, request(UUID.randomUUID())))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void deletarDeveFalharQuandoNaoExiste() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletar(id))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).delete(any(Peca.class));
	}

}
