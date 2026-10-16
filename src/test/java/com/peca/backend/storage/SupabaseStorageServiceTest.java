package com.peca.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseStorageServiceTest {

	private static final String URL = "https://projeto.supabase.co";
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

	private MockRestServiceServer server;
	private SupabaseStorageService service;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		service = new SupabaseStorageService(builder.build(),
				new SupabaseStorageProperties(URL + "/", "chave-secreta", "pecas"));
	}

	@Test
	void enviarImagemDeveFazerUploadERetornarUrlPublica() {
		server.expect(method(HttpMethod.POST))
				.andExpect(request -> assertThat(request.getURI().toString())
						.matches(URL + "/storage/v1/object/pecas/peca-1/[0-9a-f-]{36}\\.png"))
				.andExpect(header("Authorization", "Bearer chave-secreta"))
				.andExpect(header("apikey", "chave-secreta"))
				.andExpect(header("Content-Type", "image/png"))
				.andRespond(withSuccess());

		// Content-Type do cliente e ignorado: o tipo vem dos bytes do arquivo.
		String url = service.enviarImagem("peca-1", new MockMultipartFile("arquivo", "x.jpg", "image/jpeg", PNG));

		assertThat(url).matches(URL + "/storage/v1/object/public/pecas/peca-1/[0-9a-f-]{36}\\.png");
		server.verify();
	}

	@Test
	void enviarImagemDeveRejeitarArquivoQueNaoEhImagem() {
		MockMultipartFile texto = new MockMultipartFile("arquivo", "x.png", "image/png", "nao sou imagem".getBytes());

		assertThatThrownBy(() -> service.enviarImagem("peca-1", texto))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void enviarImagemDeveRejeitarArquivoVazio() {
		assertThatThrownBy(() -> service.enviarImagem("peca-1", new MockMultipartFile("arquivo", new byte[0])))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void enviarImagemDeveConverterErroDoSupabase() {
		server.expect(method(HttpMethod.POST)).andRespond(withServerError());

		assertThatThrownBy(() -> service.enviarImagem("peca-1", new MockMultipartFile("arquivo", PNG)))
				.isInstanceOf(ArmazenamentoException.class);
	}

	@Test
	void enviarImagemDeveFalharQuandoNaoConfigurado() {
		SupabaseStorageService semConfig = new SupabaseStorageService(RestClient.create(),
				new SupabaseStorageProperties("", "", "pecas"));

		assertThatThrownBy(() -> semConfig.enviarImagem("peca-1", new MockMultipartFile("arquivo", PNG)))
				.isInstanceOf(ArmazenamentoException.class);
	}

	@Test
	void removerPorUrlDeveApagarObjetoDoBucket() {
		server.expect(requestTo(URL + "/storage/v1/object/pecas/peca-1/antiga.png"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withSuccess());

		service.removerPorUrl(URL + "/storage/v1/object/public/pecas/peca-1/antiga.png");

		server.verify();
	}

	@Test
	void removerPorUrlDeveIgnorarUrlDeOutraOrigemEFalhas() {
		service.removerPorUrl("https://outro-site.com/imagem.png");
		server.verify(); // nenhuma chamada

		server.reset();
		server.expect(method(HttpMethod.DELETE)).andRespond(withServerError());
		service.removerPorUrl(URL + "/storage/v1/object/public/pecas/peca-1/antiga.png");
		server.verify();
	}

}
