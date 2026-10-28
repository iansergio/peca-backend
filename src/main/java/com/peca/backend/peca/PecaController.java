package com.peca.backend.peca;

import com.peca.backend.auth.UsuarioAutenticado;
import com.peca.backend.config.OpenApiConfig;
import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pecas")
@Tag(name = "Pecas", description = "Anuncios de pecas de celular")
public class PecaController {

	private final PecaService service;

	public PecaController(PecaService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Cria um anuncio de peca",
			description = "O vendedor e o usuario do token. O numero de contato (whatsapp) e obrigatorio e fica salvo no perfil do vendedor.")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "201", description = "Anuncio criado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos ou numero de contato ausente")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "404", description = "Usuario do token nao existe mais")
	public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		PecaResponse criada = service.criar(request, UsuarioAutenticado.id(jwt));
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
	@Operation(summary = "Edita um anuncio (somente o vendedor)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "200", description = "Anuncio atualizado")
	@ApiResponse(responseCode = "400", description = "Dados invalidos")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Anuncio pertence a outro vendedor")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	public PecaResponse atualizar(@Parameter(description = "Id do anuncio") @PathVariable UUID id,
			@Valid @RequestBody PecaRequest request, @AuthenticationPrincipal Jwt jwt) {
		return service.atualizar(id, request, UsuarioAutenticado.id(jwt));
	}

	@PostMapping(path = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@Operation(summary = "Envia a imagem de um anuncio (somente o vendedor)",
			description = "Grava a imagem no armazenamento local e salva a URL publica em imagemUrl. "
					+ "Substitui a imagem anterior, se houver. Formatos: JPEG, PNG ou WEBP, ate 5 MB.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
					mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
					encoding = @Encoding(name = "arquivo", contentType = "image/jpeg, image/png, image/webp"))))
	@ApiResponse(responseCode = "200", description = "Imagem enviada; retorna o anuncio com imagemUrl")
	@ApiResponse(responseCode = "400", description = "Arquivo ausente ou formato nao suportado")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Anuncio pertence a outro vendedor")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	@ApiResponse(responseCode = "413", description = "Arquivo maior que o limite")
	@ApiResponse(responseCode = "500", description = "Falha ao gravar a imagem")
	public PecaResponse enviarImagem(@Parameter(description = "Id do anuncio") @PathVariable UUID id,
			@Parameter(description = "Arquivo de imagem") @RequestParam("arquivo") MultipartFile arquivo,
			@AuthenticationPrincipal Jwt jwt) {
		return service.enviarImagem(id, arquivo, UsuarioAutenticado.id(jwt));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove um anuncio (somente o vendedor)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponse(responseCode = "204", description = "Anuncio removido")
	@ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
	@ApiResponse(responseCode = "403", description = "Anuncio pertence a outro vendedor")
	@ApiResponse(responseCode = "404", description = "Anuncio nao encontrado")
	public ResponseEntity<Void> deletar(@Parameter(description = "Id do anuncio") @PathVariable UUID id,
			@AuthenticationPrincipal Jwt jwt) {
		service.deletar(id, UsuarioAutenticado.id(jwt));
		return ResponseEntity.noContent().build();
	}

}
