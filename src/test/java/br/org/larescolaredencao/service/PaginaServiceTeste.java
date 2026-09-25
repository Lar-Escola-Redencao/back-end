package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.DocumentoRepository;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

@ExtendWith(MockitoExtension.class)
class PaginaServiceTeste {

    @Mock
    private PaginaRepository paginaRepository;

    @Mock
    private SecaoRepository secaoRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private ArquivoService arquivoService;

    @InjectMocks
    private PaginaService paginaService;

    private Pagina pagina;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pagina = new Pagina();
        pagina.setId(1L);
        pagina.setNome("Transparência");
        pagina.setAtivo(true);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void deveRecusarCriacaoDeSecaoQuandoPaginaNaoExiste() {
        when(paginaRepository.findById(99L)).thenReturn(Optional.empty());

        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setTitulo("Seção órfã");

        assertThatThrownBy(() -> paginaService.criarSecao(99L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(excecao -> ((ResponseStatusException) excecao).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(secaoRepository, never()).save(any());
        verify(arquivoService, never()).salvarArquivo(any(), any(), any());
    }

    @Test
    void deveCriarSecaoSomenteComTituloEConteudo() {
        when(paginaRepository.findById(1L)).thenReturn(Optional.of(pagina));
        when(arquivoService.sanitizarTexto("Relatórios Financeiros")).thenReturn("Relatórios Financeiros");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setTitulo("Relatórios Financeiros");
        dto.setConteudo("Prestação de contas de 2025.");

        Secao secao = paginaService.criarSecao(1L, dto);

        assertThat(secao.getTitulo()).isEqualTo("Relatórios Financeiros");
        assertThat(secao.getConteudo()).isEqualTo("Prestação de contas de 2025.");
        assertThat(secao.getImagem()).isNull();
        assertThat(secao.getAtivo()).isTrue();
        assertThat(secao.getPagina()).isSameAs(pagina);
        verify(arquivoService, never()).salvarArquivo(any(), any(), any());
    }

    @Test
    void deveCriarSecaoSomenteComTituloEImagem() {
        when(paginaRepository.findById(1L)).thenReturn(Optional.of(pagina));
        when(arquivoService.sanitizarTexto("Imagem Carrossel")).thenReturn("Imagem Carrossel");
        when(arquivoService.salvarArquivo(any(MultipartFile.class), eq("paginas/1/imagens/"), eq(TipoArquivo.FOTO)))
                .thenReturn("/uploads/paginas/1/imagens/foto.png");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setTitulo("Imagem Carrossel");
        dto.setImagem(imagemFake());

        Secao secao = paginaService.criarSecao(1L, dto);

        assertThat(secao.getConteudo()).isNull();
        assertThat(secao.getImagem()).isEqualTo("/uploads/paginas/1/imagens/foto.png");
        verify(arquivoService).salvarArquivo(any(MultipartFile.class), eq("paginas/1/imagens/"), eq(TipoArquivo.FOTO));
    }

    @Test
    void deveCriarSecaoComTituloConteudoEImagemNaPaginaInformada() {
        Pagina paginaSobre = new Pagina();
        paginaSobre.setId(2L);
        paginaSobre.setNome("Sobre");
        paginaSobre.setAtivo(true);

        when(paginaRepository.findById(2L)).thenReturn(Optional.of(paginaSobre));
        when(arquivoService.sanitizarTexto("Nossa História")).thenReturn("Nossa História");
        when(arquivoService.salvarArquivo(any(MultipartFile.class), eq("paginas/2/imagens/"), eq(TipoArquivo.FOTO)))
                .thenReturn("/uploads/paginas/2/imagens/historia.png");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setTitulo("Nossa História");
        dto.setConteudo("Fundado em 1965.");
        dto.setImagem(imagemFake());

        Secao secao = paginaService.criarSecao(2L, dto);

        assertThat(secao.getTitulo()).isEqualTo("Nossa História");
        assertThat(secao.getConteudo()).isEqualTo("Fundado em 1965.");
        assertThat(secao.getImagem()).isEqualTo("/uploads/paginas/2/imagens/historia.png");
        assertThat(secao.getPagina()).isSameAs(paginaSobre);
    }

    @Test
    void deveGravarConteudoComoNuloQuandoVemStringVazia() {
        when(paginaRepository.findById(1L)).thenReturn(Optional.of(pagina));
        when(arquivoService.sanitizarTexto("Aba sem texto")).thenReturn("Aba sem texto");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setTitulo("Aba sem texto");
        dto.setConteudo("   ");

        Secao secao = paginaService.criarSecao(1L, dto);

        assertThat(secao.getConteudo()).isNull();
    }

    @Test
    void deveTrocarImagemERemoverAAnteriorDoDisco() {
        Secao secao = secaoExistente("/uploads/paginas/1/imagens/antiga.png");

        when(secaoRepository.findById(10L)).thenReturn(Optional.of(secao));
        when(arquivoService.salvarArquivo(any(MultipartFile.class), eq("paginas/1/imagens/"), eq(TipoArquivo.FOTO)))
                .thenReturn("/uploads/paginas/1/imagens/nova.png");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Secao atualizada = paginaService.atualizarImagemSecao(10L, imagemFake());

        assertThat(atualizada.getImagem()).isEqualTo("/uploads/paginas/1/imagens/nova.png");
        verify(arquivoService).salvarArquivo(any(MultipartFile.class), eq("paginas/1/imagens/"), eq(TipoArquivo.FOTO));
        verify(arquivoService).deletarArquivo("/uploads/paginas/1/imagens/antiga.png");
    }

    @Test
    void naoDeveTentarRemoverImagemAnteriorQuandoSecaoNaoTinhaImagem() {
        Secao secao = secaoExistente(null);

        when(secaoRepository.findById(10L)).thenReturn(Optional.of(secao));
        when(arquivoService.salvarArquivo(any(MultipartFile.class), eq("paginas/1/imagens/"), eq(TipoArquivo.FOTO)))
                .thenReturn("/uploads/paginas/1/imagens/nova.png");
        when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Secao atualizada = paginaService.atualizarImagemSecao(10L, imagemFake());

        assertThat(atualizada.getImagem()).isEqualTo("/uploads/paginas/1/imagens/nova.png");

        ArgumentCaptor<String> caminhoRemovido = ArgumentCaptor.forClass(String.class);
        verify(arquivoService).deletarArquivo(caminhoRemovido.capture());
        assertThat(caminhoRemovido.getValue()).isNull();
    }

    @Test
    void deveListarSecoesPaginadasApenasDaPaginaInformada() {
        Page<Secao> paginaEsperada = new PageImpl<>(List.of(secaoExistente(null)), pageable, 1);
        when(secaoRepository.findByPaginaId(2L, pageable)).thenReturn(paginaEsperada);

        Page<Secao> resultado = paginaService.listarSecoesPaginado(2L, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(secaoRepository).findByPaginaId(2L, pageable);
        verify(secaoRepository, never()).findAll(pageable);
    }

    private Secao secaoExistente(String imagem) {
        Secao secao = new Secao();
        secao.setId(10L);
        secao.setTitulo("Seção existente");
        secao.setAtivo(true);
        secao.setImagem(imagem);
        secao.setPagina(pagina);
        return secao;
    }

    private MultipartFile imagemFake() {
        return new MockMultipartFile("imagem", "foto.png", "image/png", "conteudo-binario".getBytes());
    }
}
