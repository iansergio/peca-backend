package com.peca.backend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Grava as imagens das pecas no disco local, em {@code <diretorio>/<pasta>/<uuid>.<ext>},
 * e as devolve pela rota publica GET /api/imagens/{pasta}/{arquivo}.
 */
@Service
public class ArmazenamentoImagemService {

	private static final Logger log = LoggerFactory.getLogger(ArmazenamentoImagemService.class);

	// Pasta e arquivo vem da URL: so nomes gerados por este servico, sem "..", barras ou outros caracteres.
	private static final Pattern NOME_PASTA = Pattern.compile("[0-9a-fA-F-]{1,36}");
	private static final Pattern NOME_ARQUIVO = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

	private final ArmazenamentoProperties properties;

	public ArmazenamentoImagemService(ArmazenamentoProperties properties) {
		this.properties = properties;
	}

	/**
	 * Grava a imagem em {@code <pasta>/<uuid>.<ext>} e retorna a URL publica.
	 */
	public String enviarImagem(String pasta, MultipartFile arquivo) {
		byte[] bytes = lerBytes(arquivo);
		TipoImagem tipo = TipoImagem.detectar(bytes)
				.orElseThrow(() -> new ImagemInvalidaException("Formato nao suportado. Envie JPEG, PNG ou WEBP."));
		if (!NOME_PASTA.matcher(pasta).matches()) {
			throw new IllegalArgumentException("Pasta invalida: " + pasta);
		}
		String nome = UUID.randomUUID() + "." + tipo.extensao();

		try {
			Path destino = properties.diretorio().resolve(pasta);
			Files.createDirectories(destino);
			Files.write(destino.resolve(nome), bytes);
		} catch (IOException e) {
			log.error("Erro ao gravar a imagem {}/{}", pasta, nome, e);
			throw new ArmazenamentoException("Falha ao gravar a imagem: " + e.getMessage(), e);
		}
		return properties.urlPublica() + "/" + pasta + "/" + nome;
	}

	/**
	 * Le uma imagem gravada. Vazio se o nome for invalido ou o arquivo nao existir.
	 */
	public Optional<ImagemArmazenada> buscar(String pasta, String arquivo) {
		if (!NOME_PASTA.matcher(pasta).matches() || !NOME_ARQUIVO.matcher(arquivo).matches()) {
			return Optional.empty();
		}
		Path caminho = properties.diretorio().resolve(pasta).resolve(arquivo);
		if (!Files.isRegularFile(caminho)) {
			return Optional.empty();
		}
		String extensao = arquivo.substring(arquivo.lastIndexOf('.') + 1);
		return TipoImagem.porExtensao(extensao).map(tipo -> new ImagemArmazenada(caminho, tipo.contentType()));
	}

	/**
	 * Remove a imagem a partir da URL publica. Falhas sao apenas registradas em log,
	 * para nao impedir a operacao principal (ex: trocar a imagem ou excluir a peca).
	 */
	public void removerPorUrl(String urlPublica) {
		String prefixo = properties.urlPublica() + "/";
		if (urlPublica == null || !urlPublica.startsWith(prefixo)) {
			return;
		}
		String[] partes = urlPublica.substring(prefixo.length()).split("/");
		if (partes.length != 2) {
			return;
		}
		buscar(partes[0], partes[1]).ifPresent(imagem -> {
			try {
				Files.deleteIfExists(imagem.caminho());
			} catch (IOException e) {
				log.warn("Nao foi possivel remover a imagem antiga {}: {}", imagem.caminho(), e.getMessage());
			}
		});
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

	public record ImagemArmazenada(Path caminho, String contentType) {
	}

}
