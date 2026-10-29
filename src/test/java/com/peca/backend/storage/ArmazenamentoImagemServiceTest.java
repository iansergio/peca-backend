package com.peca.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ArmazenamentoImagemServiceTest {

	private static final String URL = "http://localhost:8080/api/imagens";
	private static final String PASTA = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

	@TempDir
	private Path diretorio;

	private ArmazenamentoImagemService service;

	@BeforeEach
	void setUp() {
		service = new ArmazenamentoImagemService(new ArmazenamentoProperties(diretorio, URL + "/"));
	}

	@Test
	void enviarImagemDeveGravarNoDiscoERetornarUrlPublica() throws Exception {
		// Content-Type do cliente e ignorado: o tipo vem dos bytes do arquivo.
		String url = service.enviarImagem(PASTA, new MockMultipartFile("arquivo", "x.jpg", "image/jpeg", PNG));

		assertThat(url).matches(URL + "/" + PASTA + "/[0-9a-f-]{36}\\.png");
		String arquivo = url.substring(url.lastIndexOf('/') + 1);
		assertThat(Files.readAllBytes(diretorio.resolve(PASTA).resolve(arquivo))).isEqualTo(PNG);
	}

	@Test
	void enviarImagemDeveRejeitarArquivoQueNaoEhImagem() {
		MockMultipartFile texto = new MockMultipartFile("arquivo", "x.png", "image/png", "nao sou imagem".getBytes());

		assertThatThrownBy(() -> service.enviarImagem(PASTA, texto))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void enviarImagemDeveRejeitarArquivoVazio() {
		assertThatThrownBy(() -> service.enviarImagem(PASTA, new MockMultipartFile("arquivo", new byte[0])))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void buscarDeveDevolverImagemGravadaComContentType() {
		String url = service.enviarImagem(PASTA, new MockMultipartFile("arquivo", PNG));
		String arquivo = url.substring(url.lastIndexOf('/') + 1);

		assertThat(service.buscar(PASTA, arquivo))
				.hasValueSatisfying(imagem -> assertThat(imagem.contentType()).isEqualTo("image/png"));
	}

	@Test
	void buscarDeveRecusarNomesForaDoPadrao() {
		assertThat(service.buscar("..", "segredo.png")).isEmpty();
		assertThat(service.buscar(PASTA, "../../etc/passwd")).isEmpty();
		assertThat(service.buscar(PASTA, "00000000-0000-0000-0000-000000000000.png")).isEmpty();
	}

	@Test
	void removerPorUrlDeveApagarArquivoEIgnorarUrlDeOutraOrigem() {
		String url = service.enviarImagem(PASTA, new MockMultipartFile("arquivo", PNG));
		String arquivo = url.substring(url.lastIndexOf('/') + 1);

		service.removerPorUrl("https://outro-site.com/imagem.png");
		assertThat(service.buscar(PASTA, arquivo)).isPresent();

		service.removerPorUrl(url);
		assertThat(service.buscar(PASTA, arquivo)).isEmpty();
	}

}
