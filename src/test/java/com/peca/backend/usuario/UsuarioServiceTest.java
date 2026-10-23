package com.peca.backend.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.ConflitoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.usuario.dto.UsuarioRequest;
import com.peca.backend.usuario.dto.UsuarioResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository repository;

	private UsuarioService service;

	@BeforeEach
	void setUp() {
		service = new UsuarioService(repository);
	}

	private static UsuarioRequest request(String email) {
		return new UsuarioRequest("Maria Silva", email, "11999998888");
	}

	private static Usuario usuarioExistente(UUID id, String email) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Maria Silva");
		usuario.setEmail(email);
		return usuario;
	}

	@Test
	void buscarPorIdDeveFalharQuandoNaoExiste() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(id, id))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void buscarPorIdDeveFalharQuandoUsuarioLogadoEhOutro() {
		assertThatThrownBy(() -> service.buscarPorId(UUID.randomUUID(), UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).findById(any());
	}

	@Test
	void buscarAtualDeveRetornarOUsuarioDoToken() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(usuarioExistente(id, "maria@email.com")));

		UsuarioResponse response = service.buscarAtual(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.email()).isEqualTo("maria@email.com");
	}

	@Test
	void buscarAtualDeveFalharQuandoUsuarioDoTokenNaoExisteMais() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarAtual(id))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void atualizarDevePermitirManterOProprioEmail() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(usuarioExistente(id, "maria@email.com")));
		when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

		UsuarioResponse response = service.atualizar(id, request("maria@email.com"), id);

		assertThat(response.id()).isEqualTo(id);
		verify(repository, never()).existsByEmail(any());
	}

	@Test
	void atualizarDeveFalharQuandoNovoEmailPertenceAOutroUsuario() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(usuarioExistente(id, "maria@email.com")));
		when(repository.existsByEmail("outro@email.com")).thenReturn(true);

		assertThatThrownBy(() -> service.atualizar(id, request("outro@email.com"), id))
				.isInstanceOf(ConflitoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void deletarDeveFalharQuandoNaoExiste() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletar(id, id))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).delete(any());
	}

	@Test
	void atualizarDeveFalharQuandoUsuarioLogadoEhOutro() {
		assertThatThrownBy(() -> service.atualizar(UUID.randomUUID(), request("maria@email.com"), UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void deletarDeveFalharQuandoUsuarioLogadoEhOutro() {
		assertThatThrownBy(() -> service.deletar(UUID.randomUUID(), UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).delete(any());
	}

}
