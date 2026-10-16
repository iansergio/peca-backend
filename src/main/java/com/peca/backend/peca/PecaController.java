package com.peca.backend.peca;

import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pecas")
@Tag(name = "Pecas", description = "Anuncios de pecas de celular")
public class PecaController {

	private final PecaService service;

	public PecaController(PecaService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Cria um anuncio de peca")
	@ApiResponse(responseCode = "201", description = "Anuncio criado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "404", description = "Vendedor nao encontrado")
	public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
		PecaResponse criada = service.criar(request);
		return ResponseEntity.created(URI.create("/api/pecas/" + criada.id())).body(criada);
	}

	@GetMapping
	@Operation(summary = "Lista anuncios (publico)",
			description = "Filtros opcionais por modelo de celular (busca parcial, sem diferenciar maiusculas) e preco maximo. Ordenado por preco crescente.")
	@ApiResponse(responseCode = "200", description = "Lista de anuncios")
	public List<PecaResponse> listar(
			@Parameter(description = "Modelo de celular", example = "iPhone 13")
			@RequestParam(required = false) String modeloCelular,
			@Parameter(description = "Preco maximo (inclusive)", example = "500.00")
			@RequestParam(required = false) BigDecimal precoMaximo) {
		return service.listar(modeloCelular, precoMaximo);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um anuncio pelo id")
	@ApiResponse(responseCode = "200", description = "Anuncio encontrado")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	public PecaResponse buscarPorId(@Parameter(description = "Id do anuncio") @PathVariable UUID id) {
		return service.buscarPorId(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Edita um anuncio")
	@ApiResponse(responseCode = "200", description = "Anuncio atualizado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "403", description = "vendedorId diferente do vendedor do anuncio")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	public PecaResponse atualizar(@Parameter(description = "Id do anuncio") @PathVariable UUID id,
			@Valid @RequestBody PecaRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove um anuncio")
	@ApiResponse(responseCode = "204", description = "Anuncio removido")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	public ResponseEntity<Void> deletar(@Parameter(description = "Id do anuncio") @PathVariable UUID id) {
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}

}
