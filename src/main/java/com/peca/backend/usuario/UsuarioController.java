package com.peca.backend.usuario;

import com.peca.backend.usuario.dto.UsuarioRequest;
import com.peca.backend.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "CRUD de usuarios")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Cria um usuario")
	@ApiResponse(responseCode = "201", description = "Usuario criado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "409", description = "Email ja cadastrado")
	public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
		UsuarioResponse criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
	}

	@GetMapping
	@Operation(summary = "Lista todos os usuarios")
	@ApiResponse(responseCode = "200", description = "Lista de usuarios")
	public List<UsuarioResponse> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca um usuario pelo id")
	@ApiResponse(responseCode = "200", description = "Usuario encontrado")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	public UsuarioResponse buscarPorId(@Parameter(description = "Id do usuario") @PathVariable UUID id) {
		return service.buscarPorId(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza um usuario")
	@ApiResponse(responseCode = "200", description = "Usuario atualizado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	@ApiResponse(responseCode = "409", description = "Email ja cadastrado")
	public UsuarioResponse atualizar(@Parameter(description = "Id do usuario") @PathVariable UUID id,
			@Valid @RequestBody UsuarioRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove um usuario")
	@ApiResponse(responseCode = "204", description = "Usuario removido")
	@ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
	public ResponseEntity<Void> deletar(@Parameter(description = "Id do usuario") @PathVariable UUID id) {
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}

}
