package com.peca.backend.peca;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PecaService {

	private final PecaRepository repository;
	private final UsuarioRepository usuarioRepository;

	public PecaService(PecaRepository repository, UsuarioRepository usuarioRepository) {
		this.repository = repository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	public PecaResponse criar(PecaRequest request) {
		Usuario vendedor = usuarioRepository.findById(request.vendedorId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Vendedor nao encontrado: " + request.vendedorId()));
		Peca peca = new Peca();
		peca.setVendedor(vendedor);
		aplicar(peca, request);
		return PecaResponse.from(repository.save(peca));
	}

	@Transactional(readOnly = true)
	public List<PecaResponse> listar(String modeloCelular, BigDecimal precoMaximo) {
		return repository.findAll(PecaSpecifications.filtrar(modeloCelular, precoMaximo), Sort.by("preco"))
				.stream()
				.map(PecaResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PecaResponse buscarPorId(UUID id) {
		return PecaResponse.from(buscarEntidade(id));
	}

	@Transactional
	public PecaResponse atualizar(UUID id, PecaRequest request) {
		Peca peca = buscarEntidade(id);
		// Sem JWT ainda, o vendedor e identificado pelo vendedorId do corpo.
		if (!peca.getVendedor().getId().equals(request.vendedorId())) {
			throw new AcessoNegadoException("Somente o vendedor do anuncio pode edita-lo");
		}
		aplicar(peca, request);
		return PecaResponse.from(repository.save(peca));
	}

	@Transactional
	public void deletar(UUID id) {
		repository.delete(buscarEntidade(id));
	}

	private Peca buscarEntidade(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Peca nao encontrada: " + id));
	}

	private static void aplicar(Peca peca, PecaRequest request) {
		peca.setTitulo(request.titulo().trim());
		peca.setDescricao(request.descricao());
		peca.setPreco(request.preco());
		peca.setCategoria(request.categoria().trim());
		peca.setModeloCelular(request.modeloCelular().trim());
	}

}
