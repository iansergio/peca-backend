package com.peca.backend.peca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peca.backend.exception.AcessoNegadoException;
import com.peca.backend.exception.RecursoNaoEncontradoException;
import com.peca.backend.peca.dto.PecaRequest;
import com.peca.backend.peca.dto.PecaResponse;
import com.peca.backend.storage.SupabaseStorageService;
import com.peca.backend.usuario.Usuario;
import com.peca.backend.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

	@Mock
	private PecaRepository repository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private SupabaseStorageService storageService;

	private PecaService service;

	@BeforeEach
	void setUp() {
		service = new PecaService(repository, usuarioRepository, storageService);
	}

	private static Usuario vendedor(UUID id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Maria Silva");
		usuario.setEmail("maria@email.com");
		usuario.setWhatsapp("11999998888");
		return usuario;
	}

	private static PecaRequest request() {
		return new PecaRequest(" Tela iPhone 13 ", "Original", new BigDecimal("450.00"), "Tela", "iPhone 13", "11988887777");
	}

	private static Peca pecaExistente(UUID id, Usuario vendedor) {
		Peca peca = new Peca();
		peca.setId(id);
		peca.setTitulo("Antigo");
		peca.setPreco(BigDecimal.TEN);
		peca.setCategoria("Tela");
		peca.setModeloCelular("iPhone 12");
		peca.setVendedor(vendedor);
		return peca;
	}

	@Test
	void criarDeveVincularVendedorERetornarSeusDadosPublicos() {
		UUID vendedorId = UUID.randomUUID();
		when(usuarioRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor(vendedorId)));
		when(repository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

		PecaResponse response = service.criar(request(), vendedorId);

		assertThat(response.titulo()).isEqualTo("Tela iPhone 13");
		assertThat(response.vendedor().id()).isEqualTo(vendedorId);
		assertThat(response.vendedor().nome()).isEqualTo("Maria Silva");
		assertThat(response.vendedor().whatsapp()).isEqualTo("11988887777");
	}

	@Test
	void criarDeveFalharQuandoVendedorNaoExiste() {
		UUID vendedorId = UUID.randomUUID();
		when(usuarioRepository.findById(vendedorId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.criar(request(), vendedorId))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void atualizarDeveAlterarCamposQuandoVendedorEhODono() {
		UUID id = UUID.randomUUID();
		UUID vendedorId = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(vendedorId))));
		when(repository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

		PecaResponse response = service.atualizar(id, request(), vendedorId);

		assertThat(response.modeloCelular()).isEqualTo("iPhone 13");
		assertThat(response.preco()).isEqualByComparingTo("450.00");
	}

	@Test
	void atualizarDeveFalharQuandoVendedorEhOutro() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(UUID.randomUUID()))));

		assertThatThrownBy(() -> service.atualizar(id, request(), UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void enviarImagemDeveSalvarUrlERemoverImagemAnterior() {
		UUID id = UUID.randomUUID();
		UUID vendedorId = UUID.randomUUID();
		Peca peca = pecaExistente(id, vendedor(vendedorId));
		peca.setImagemUrl("https://x.supabase.co/storage/v1/object/public/pecas/antiga.png");
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[] {1});
		when(repository.findById(id)).thenReturn(Optional.of(peca));
		when(storageService.enviarImagem(id.toString(), arquivo)).thenReturn("https://x.supabase.co/nova.png");
		when(repository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

		PecaResponse response = service.enviarImagem(id, arquivo, vendedorId);

		assertThat(response.imagemUrl()).isEqualTo("https://x.supabase.co/nova.png");
		verify(storageService).removerPorUrl("https://x.supabase.co/storage/v1/object/public/pecas/antiga.png");
	}

	@Test
	void enviarImagemDeveFalharSemUploadQuandoPecaNaoExiste() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.enviarImagem(id, new MockMultipartFile("arquivo", new byte[] {1}), UUID.randomUUID()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(storageService, never()).enviarImagem(any(), any());
	}

	@Test
	void enviarImagemDeveFalharSemUploadQuandoVendedorEhOutro() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(UUID.randomUUID()))));

		assertThatThrownBy(() -> service.enviarImagem(id, new MockMultipartFile("arquivo", new byte[] {1}), UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(storageService, never()).enviarImagem(any(), any());
	}

	@Test
	void deletarDeveFalharQuandoVendedorEhOutro() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(pecaExistente(id, vendedor(UUID.randomUUID()))));

		assertThatThrownBy(() -> service.deletar(id, UUID.randomUUID()))
				.isInstanceOf(AcessoNegadoException.class);
		verify(repository, never()).delete(any(Peca.class));
	}

	@Test
	void deletarDeveFalharQuandoNaoExiste() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deletar(id, UUID.randomUUID()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).delete(any(Peca.class));
	}

}
