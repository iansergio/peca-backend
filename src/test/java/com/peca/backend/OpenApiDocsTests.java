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
				.andExpect(jsonPath("$.paths['/api/usuarios']").exists())
				.andExpect(jsonPath("$.paths['/api/usuarios/{id}']").exists())
				.andExpect(jsonPath("$.components.schemas.UsuarioResponse.properties.senha").doesNotExist());
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

}
