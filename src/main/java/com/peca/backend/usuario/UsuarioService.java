package com.peca.backend.usuario;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.usuario.dto.UsuarioRequest;
import com.peca.backend.usuario.dto.UsuarioResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository repository;

	public UsuarioService(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarAtual(UUID usuarioLogadoId) {
		return UsuarioResponse.from(buscarEntidade(usuarioLogadoId));
	}

	// O perfil inclui email e so pode ser lido pelo proprio usuario.
	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(UUID id, UUID usuarioLogadoId) {
		exigirProprioUsuario(id, usuarioLogadoId);
		return UsuarioResponse.from(buscarEntidade(id));
	}

	// Transacao propria: se uma requisicao concorrente ja cadastrou o email, a violacao da unique
	// nao inutiliza a transacao de quem chamou, que pode entao buscar o usuario existente.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Usuario cadastrar(String nome, String email) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome.trim());
		usuario.setEmail(normalizarEmail(email));
		return repository.saveAndFlush(usuario);
	}

	@Transactional
	public UsuarioResponse atualizar(UUID id, UsuarioRequest request, UUID usuarioLogadoId) {
		exigirProprioUsuario(id, usuarioLogadoId);
		Usuario usuario = buscarEntidade(id);
		aplicar(usuario, request);
		return UsuarioResponse.from(repository.save(usuario));
	}

	@Transactional
	public void deletar(UUID id, UUID usuarioLogadoId) {
		exigirProprioUsuario(id, usuarioLogadoId);
		repository.delete(buscarEntidade(id));
	}

	private static void exigirProprioUsuario(UUID id, UUID usuarioLogadoId) {
		if (!id.equals(usuarioLogadoId)) {
			throw new AcessoNegadoException("Voce so pode acessar ou alterar a sua propria conta");
		}
	}

	private Usuario buscarEntidade(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
	}

	// O email nao e editavel: ele e a identidade confirmada por OTP.
	private void aplicar(Usuario usuario, UsuarioRequest request) {
		usuario.setNome(request.nome().trim());
		usuario.setWhatsapp(request.whatsapp());
	}

	public static String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}

}
