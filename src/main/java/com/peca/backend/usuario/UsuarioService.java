package com.peca.backend.usuario;

import com.peca.backend.exception.ConflitoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.usuario.dto.UsuarioRequest;
import com.peca.backend.usuario.dto.UsuarioResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		String email = normalizarEmail(request.email());
		if (repository.existsByEmail(email)) {
			throw new ConflitoException("Email ja cadastrado");
		}
		Usuario usuario = new Usuario();
		aplicar(usuario, request, email);
		return UsuarioResponse.from(repository.save(usuario));
	}

	@Transactional(readOnly = true)
	public List<UsuarioResponse> listar() {
		return repository.findAll().stream().map(UsuarioResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(UUID id) {
		return UsuarioResponse.from(buscarEntidade(id));
	}

	@Transactional
	public UsuarioResponse atualizar(UUID id, UsuarioRequest request) {
		Usuario usuario = buscarEntidade(id);
		String email = normalizarEmail(request.email());
		if (!email.equals(usuario.getEmail()) && repository.existsByEmail(email)) {
			throw new ConflitoException("Email ja cadastrado");
		}
		aplicar(usuario, request, email);
		return UsuarioResponse.from(repository.save(usuario));
	}

	@Transactional
	public void deletar(UUID id) {
		repository.delete(buscarEntidade(id));
	}

	private Usuario buscarEntidade(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
	}

	private void aplicar(Usuario usuario, UsuarioRequest request, String email) {
		usuario.setNome(request.nome().trim());
		usuario.setEmail(email);
		usuario.setSenha(passwordEncoder.encode(request.senha()));
		usuario.setWhatsapp(request.whatsapp());
	}

	private static String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}

}
