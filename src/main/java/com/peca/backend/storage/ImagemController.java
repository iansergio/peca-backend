package com.peca.backend.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imagens")
@Tag(name = "Imagens", description = "Fotos dos anuncios")
public class ImagemController {

	private final ArmazenamentoImagemService service;

	public ImagemController(ArmazenamentoImagemService service) {
		this.service = service;
	}

	@GetMapping("/{pasta}/{arquivo}")
	@Operation(summary = "Devolve a foto de um anuncio (publico)",
			description = "Caminho igual ao final do imagemUrl do anuncio. O nome do arquivo e unico, entao a resposta e cacheavel.")
	@ApiResponse(responseCode = "200", description = "Imagem JPEG, PNG ou WEBP")
	@ApiResponse(responseCode = "404", description = "Imagem nao encontrada")
	public ResponseEntity<Resource> buscar(@Parameter(description = "Id do anuncio") @PathVariable String pasta,
			@Parameter(description = "Nome do arquivo") @PathVariable String arquivo) {
		return service.buscar(pasta, arquivo)
				.map(imagem -> ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(imagem.contentType()))
						.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
						.<Resource>body(new FileSystemResource(imagem.caminho())))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

}
