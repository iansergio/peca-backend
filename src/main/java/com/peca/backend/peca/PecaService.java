package com.peca.backend.peca;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import com.peca.backend.storage.ArmazenamentoImagemService;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PecaService {

	private final PecaRepository repository;
	private final UsuarioRepository usuarioRepository;
	private final ArmazenamentoImagemService storageService;

	public PecaService(PecaRepository repository, UsuarioRepository usuarioRepository,
			ArmazenamentoImagemService storageService) {
		this.repository = repository;
		this.usuarioRepository = usuarioRepository;
		this.storageService = storageService;
	}

	@Transactional
	public PecaResponse criar(PecaRequest request, UUID vendedorId) {
		Usuario vendedor = usuarioRepository.findById(vendedorId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Vendedor nao encontrado: " + vendedorId));
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
	public PecaResponse atualizar(UUID id, PecaRequest request, UUID usuarioLogadoId) {
		Peca peca = buscarDoVendedor(id, usuarioLogadoId);
		aplicar(peca, request);
		return PecaResponse.from(repository.save(peca));
	}

	@Transactional
	public PecaResponse enviarImagem(UUID id, MultipartFile arquivo, UUID usuarioLogadoId) {
		Peca peca = buscarDoVendedor(id, usuarioLogadoId);
		String imagemAnterior = peca.getImagemUrl();
		peca.setImagemUrl(storageService.enviarImagem(id.toString(), arquivo));
		PecaResponse response = PecaResponse.from(repository.save(peca));
		storageService.removerPorUrl(imagemAnterior);
		return response;
	}

	@Transactional
	public void deletar(UUID id, UUID usuarioLogadoId) {
		Peca peca = buscarDoVendedor(id, usuarioLogadoId);
		repository.delete(peca);
		storageService.removerPorUrl(peca.getImagemUrl());
	}

	private Peca buscarDoVendedor(UUID id, UUID usuarioLogadoId) {
		Peca peca = buscarEntidade(id);
		if (!peca.getVendedor().getId().equals(usuarioLogadoId)) {
			throw new AcessoNegadoException("Somente o vendedor do anuncio pode altera-lo");
		}
		return peca;
	}

	private Peca buscarEntidade(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Peca nao encontrada: " + id));
	}

	// O contato do anuncio e o WhatsApp do vendedor, exibido publicamente em PecaResponse.vendedor.
	private static void aplicar(Peca peca, PecaRequest request) {
		peca.setTitulo(request.titulo().trim());
		peca.setDescricao(request.descricao());
		peca.setPreco(request.preco());
		peca.setCategoria(request.categoria().trim());
		peca.setModeloCelular(request.modeloCelular().trim());
		peca.getVendedor().setWhatsapp(request.whatsapp());
	}

}
