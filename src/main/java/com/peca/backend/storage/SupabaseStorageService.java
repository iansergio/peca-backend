package com.peca.backend.storage;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Integracao com a API REST do Supabase Storage.
 * O bucket precisa ser publico para que a URL retornada seja acessivel sem autenticacao.
 */
@Service
public class SupabaseStorageService {

	private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

	private final RestClient restClient;
	private final SupabaseStorageProperties properties;

	public SupabaseStorageService(RestClient supabaseStorageRestClient, SupabaseStorageProperties properties) {
		this.restClient = supabaseStorageRestClient;
		this.properties = properties;
	}

	/**
	 * Envia a imagem para {@code <bucket>/<pasta>/<uuid>.<ext>} e retorna a URL publica.
	 */
	public String enviarImagem(String pasta, MultipartFile arquivo) {
		verificarConfiguracao();
		byte[] bytes = lerBytes(arquivo);
		TipoImagem tipo = TipoImagem.detectar(bytes)
				.orElseThrow(() -> new ImagemInvalidaException("Formato nao suportado. Envie JPEG, PNG ou WEBP."));
		String caminho = pasta + "/" + UUID.randomUUID() + "." + tipo.extensao();

		try {
			restClient.post()
					.uri(objetoUri(caminho))
					.headers(h -> {
						h.setBearerAuth(properties.serviceKey());
						h.set("apikey", properties.serviceKey());
					})
					.contentType(MediaType.parseMediaType(tipo.contentType()))
					.body(bytes)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			throw new ArmazenamentoException("Falha ao enviar imagem para o Supabase Storage", e);
		}
		return urlPublica(caminho);
	}

	/**
	 * Remove a imagem a partir da URL publica. Falhas sao apenas registradas em log,
	 * para nao impedir a operacao principal (ex: trocar a imagem ou excluir a peca).
	 */
	public void removerPorUrl(String urlPublica) {
		if (urlPublica == null || !properties.configurado()) {
			return;
		}
		String prefixo = urlPublica(""); // .../object/public/<bucket>/
		if (!urlPublica.startsWith(prefixo)) {
			return;
		}
		String caminho = urlPublica.substring(prefixo.length());
		try {
			restClient.delete()
					.uri(objetoUri(caminho))
					.headers(h -> {
						h.setBearerAuth(properties.serviceKey());
						h.set("apikey", properties.serviceKey());
					})
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			log.warn("Nao foi possivel remover a imagem antiga {}: {}", caminho, e.getMessage());
		}
	}

	private URI objetoUri(String caminho) {
		return UriComponentsBuilder.fromUriString(properties.urlBase())
				.pathSegment("storage", "v1", "object", properties.bucket())
				.pathSegment(caminho.split("/"))
				.encode()
				.build()
				.toUri();
	}

	private String urlPublica(String caminho) {
		return properties.urlBase() + "/storage/v1/object/public/" + properties.bucket() + "/" + caminho;
	}

	private void verificarConfiguracao() {
		if (!properties.configurado()) {
			throw new ArmazenamentoException(
					"Supabase Storage nao configurado (defina SUPABASE_URL, SUPABASE_SERVICE_KEY e SUPABASE_STORAGE_BUCKET)");
		}
	}

	private static byte[] lerBytes(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ImagemInvalidaException("Arquivo de imagem vazio ou ausente");
		}
		try {
			return arquivo.getBytes();
		} catch (IOException e) {
			throw new ImagemInvalidaException("Nao foi possivel ler o arquivo enviado");
		}
	}

}
