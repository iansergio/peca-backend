package com.peca.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsDeveExporEndpointsDeUsuarios() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/usuarios']").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/usuarios/me'].get").exists())
				.andExpect(jsonPath("$.paths['/api/usuarios/{id}'].get.responses['403']").exists())
				.andExpect(jsonPath("$.components.schemas.UsuarioResponse.properties.senha").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.UsuarioRequest.properties.senha").doesNotExist());
	}

	@Test
	void apiDocsDeveExporEndpointsDePecas() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/pecas'].get").exists())
				.andExpect(jsonPath("$.paths['/api/pecas'].post").exists())
				.andExpect(jsonPath("$.paths['/api/pecas/{id}'].put").exists())
				.andExpect(jsonPath("$.paths['/api/pecas/{id}'].delete").exists())
				.andExpect(jsonPath("$.components.schemas.VendedorResponse.properties.senha").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.VendedorResponse.properties.email").doesNotExist());
	}

	@Test
	void apiDocsDeveDocumentarUploadDeImagemComoMultipart() throws Exception {
		String schema = "$.paths['/api/pecas/{id}/imagem'].post.requestBody.content['multipart/form-data'].schema";
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(schema + ".properties.arquivo.format").value("binary"))
				.andExpect(jsonPath("$.components.schemas.PecaResponse.properties.imagemUrl").exists());
	}

	@Test
	void apiDocsDeveDocumentarAutenticacaoBearer() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.paths['/api/pecas'].post.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/pecas'].get.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/auth/solicitar-codigo'].post").exists())
				.andExpect(jsonPath("$.paths['/api/auth/solicitar-codigo'].post.responses['429'].headers['Retry-After']").exists())
				.andExpect(jsonPath("$.paths['/api/auth/verificar-codigo'].post").exists())
				.andExpect(jsonPath("$.paths['/api/auth/login']").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.PecaRequest.properties.vendedorId").doesNotExist());
	}

}
