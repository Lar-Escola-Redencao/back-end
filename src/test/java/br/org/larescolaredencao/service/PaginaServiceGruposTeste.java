package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.DocumentoRepository;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

/**
 * Regras dos grupos da Gráfica (telefone, produtos) e do Pix (pix). Os repositórios são simulados em
 * memória e o storage é o ArquivoService real apontando para um diretório temporário,
 * para verificar de fato quais arquivos ficam ou saem do disco.
 */
@ExtendWith(MockitoExtension.class)
class PaginaServiceGruposTeste {

    private static final Long PAGINA_TRANSPARENCIA = 1L;
    private static final Long PAGINA_GRAFICA = 3L;
    private static final Long PAGINA_PIX = 4L;

    @Mock
    private PaginaRepository paginaRepository;

    @Mock
    private SecaoRepository secaoRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @TempDir
    private Path diretorioUploads;

    private ArquivoService arquivoService;
    private PaginaService paginaService;

    private final List<Secao> secoesGravadas = new ArrayList<>();
    private final AtomicLong sequenciaSecao = new AtomicLong();

    @BeforeEach
    void setUp() {
        arquivoService = spy(new ArquivoService());
        ReflectionTestUtils.setField(arquivoService, "uploadDir", diretorioUploads.toString() + "/");
        paginaService = new PaginaService(paginaRepository, secaoRepository, documentoRepository, arquivoService);

        Map<Long, Pagina> paginas = Map.of(
                PAGINA_TRANSPARENCIA, pagina(PAGINA_TRANSPARENCIA, "Transparência"),
                PAGINA_GRAFICA, pagina(PAGINA_GRAFICA, "Gráfica"),
                PAGINA_PIX, pagina(PAGINA_PIX, "Pix"));

        lenient().when(paginaRepository.findById(anyLong()))
                .thenAnswer(invocacao -> Optional.ofNullable(paginas.get(invocacao.<Long>getArgument(0))));
        lenient().when(paginaRepository.findByIdComBloqueio(anyLong()))
                .thenAnswer(invocacao -> Optional.ofNullable(paginas.get(invocacao.<Long>getArgument(0))));

        lenient().when(secaoRepository.save(any(Secao.class))).thenAnswer(invocacao -> {
            Secao secao = invocacao.getArgument(0);
            if (secao.getId() == null) {
                secao.setId(sequenciaSecao.incrementAndGet());
                secoesGravadas.add(secao);
            }
            return secao;
        });
        lenient().when(secaoRepository.findById(anyLong())).thenAnswer(invocacao -> secoesGravadas.stream()
                .filter(secao -> secao.getId().equals(invocacao.getArgument(0)))
                .findFirst());
        lenient().when(secaoRepository.findByIdAndPaginaId(anyLong(), anyLong())).thenAnswer(invocacao -> secoesGravadas.stream()
                .filter(secao -> secao.getId().equals(invocacao.getArgument(0)))
                .filter(secao -> secao.getPagina().getId().equals(invocacao.getArgument(1)))
                .findFirst());
        lenient().when(secaoRepository.findFirstByPaginaIdAndGrupo(anyLong(), anyString())).thenAnswer(invocacao -> secoesGravadas.stream()
                .filter(secao -> secao.getPagina().getId().equals(invocacao.getArgument(0)))
                .filter(secao -> secao.getGrupo().equals(invocacao.getArgument(1)))
                .findFirst());
        lenient().when(secaoRepository.findMaxOrdemByPaginaIdAndGrupo(anyLong(), anyString())).thenAnswer(invocacao -> secoesGravadas.stream()
                .filter(secao -> secao.getPagina().getId().equals(invocacao.getArgument(0)))
                .filter(secao -> secao.getGrupo().equals(invocacao.getArgument(1)))
                .map(Secao::getOrdem)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null));
    }

    @AfterEach
    void limparSincronizacao() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------------------------------------------------------------- critérios de aceite

    @Test
    void telefoneDeveGravarWhatsappNoTituloAlternativoNoConteudoEImagemNula() {
        Secao secao = paginaService.criarSecao(PAGINA_GRAFICA, criar(" Telefone ", "16999999999", "1633333333", null));

        assertThat(secao.getPagina().getId()).isEqualTo(PAGINA_GRAFICA);
        assertThat(secao.getGrupo()).isEqualTo("telefone");
        assertThat(secao.getTitulo()).isEqualTo("16999999999");
        assertThat(secao.getConteudo()).isEqualTo("1633333333");
        assertThat(secao.getImagem()).isNull();
    }

    @Test
    void telefoneAlternativoEhOpcional() {
        Secao secao = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", "  ", null));

        assertThat(secao.getConteudo()).isNull();
    }

    @Test
    void doisPostsDeTelefoneDevemResultarEmUmUnicoRegistroAtualizado() {
        Secao primeiro = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", "1633333333", null));
        Secao segundo = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16988888888", null, null));

        assertThat(secoesGravadas).hasSize(1);
        assertThat(segundo.getId()).isEqualTo(primeiro.getId());
        assertThat(segundo.getTitulo()).isEqualTo("16988888888");
        assertThat(segundo.getConteudo()).isNull();
        verify(paginaRepository, times(2)).findByIdComBloqueio(PAGINA_GRAFICA);
    }

    @Test
    void produtoViaMultipartDeveGravarNomeNoTituloECaminhoDoUploadNaImagem() {
        Secao secao = paginaService.criarSecao(PAGINA_GRAFICA,
                criar("produtos", "Folder A3", "texto que o grupo não usa", imagem("folder.png")));

        assertThat(secao.getPagina().getId()).isEqualTo(PAGINA_GRAFICA);
        assertThat(secao.getGrupo()).isEqualTo("produtos");
        assertThat(secao.getTitulo()).isEqualTo("Folder A3");
        assertThat(secao.getConteudo()).isNull();
        assertThat(secao.getImagem()).startsWith("/uploads/paginas/3/imagens/").endsWith("_folder.png");
        assertThat(arquivoNoDisco(secao.getImagem())).exists();
    }

    @Test
    void produtosSempreCriamNovoRegistro() {
        Secao primeiro = paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", "Folder A3", null, imagem("a.png")));
        Secao segundo = paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", "Cartão de visita", null, imagem("b.png")));

        assertThat(secoesGravadas).hasSize(2);
        assertThat(segundo.getId()).isNotEqualTo(primeiro.getId());
        assertThat(segundo.getOrdem()).isEqualTo(primeiro.getOrdem() + 1);
    }

    @Test
    void pixDeveAtualizarRegistroUnicoComChaveEQrCodeERemoverArquivoAntigoAposCommit() {
        Secao pix = paginaService.criarSecao(PAGINA_PIX, criar("pix", "título ignorado", "12.345.678/0001-90", imagem("qr-antigo.png")));
        String qrAntigo = pix.getImagem();
        assertThat(pix.getTitulo()).isNull();
        assertThat(arquivoNoDisco(qrAntigo)).exists();

        Secao atualizado = emTransacao(true, () -> paginaService.atualizarSecao(PAGINA_PIX, pix.getId(),
                atualizar("pix", "título ignorado", "email@larredencao.org.br", imagem("qr-novo.png"))));

        assertThat(secoesGravadas).hasSize(1);
        assertThat(atualizado.getId()).isEqualTo(pix.getId());
        assertThat(atualizado.getPagina().getId()).isEqualTo(PAGINA_PIX);
        assertThat(atualizado.getGrupo()).isEqualTo("pix");
        assertThat(atualizado.getTitulo()).isNull();
        assertThat(atualizado.getConteudo()).isEqualTo("email@larredencao.org.br");
        assertThat(atualizado.getImagem()).isNotEqualTo(qrAntigo).endsWith("_qr-novo.png");
        assertThat(arquivoNoDisco(atualizado.getImagem())).exists();
        assertThat(arquivoNoDisco(qrAntigo)).doesNotExist();
    }

    @Test
    void segundoPostDePixTambemEhUpsertETrocaOQrCode() {
        Secao primeiro = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave-antiga", imagem("qr-1.png")));
        String qrAntigo = primeiro.getImagem();

        Secao segundo = emTransacao(true, () -> paginaService.criarSecao(PAGINA_PIX,
                criar("pix", null, "chave-nova", imagem("qr-2.png"))));

        assertThat(secoesGravadas).hasSize(1);
        assertThat(segundo.getId()).isEqualTo(primeiro.getId());
        assertThat(segundo.getConteudo()).isEqualTo("chave-nova");
        assertThat(arquivoNoDisco(qrAntigo)).doesNotExist();
        assertThat(arquivoNoDisco(segundo.getImagem())).exists();
    }

    @Test
    void segundoPostDePixSemArquivoMantemQrCodeAtual() {
        Secao primeiro = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave-antiga", imagem("qr-1.png")));
        String qrAtual = primeiro.getImagem();

        Secao segundo = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave-nova", null));

        assertThat(segundo.getImagem()).isEqualTo(qrAtual);
        assertThat(arquivoNoDisco(qrAtual)).exists();
    }

    // ---------------------------------------------------------------- validações de grupo/página

    @Test
    void deveRetornar404QuandoPaginaNaoExiste() {
        assertStatus(() -> paginaService.criarSecao(99L, criar("telefone", "16999999999", null, null)),
                HttpStatus.NOT_FOUND, "Página não encontrada");
        assertStatus(() -> paginaService.atualizarSecao(99L, 1L, atualizar("telefone", "16999999999", null, null)),
                HttpStatus.NOT_FOUND, "Página não encontrada");
    }

    @Test
    void deveRetornar400ParaGrupoInexistenteNaPaginaConfigurada() {
        assertStatus(() -> paginaService.criarSecao(PAGINA_GRAFICA, criar("banner", "Algum título", null, null)),
                HttpStatus.BAD_REQUEST, "Grupo 'banner' inválido para esta página. Grupos aceitos: telefone, produtos.");
    }

    @Test
    void deveRetornar400QuandoGrupoNaoEhInformadoNaPaginaConfigurada() {
        assertStatus(() -> paginaService.criarSecao(PAGINA_PIX, criar("  ", null, "chave", imagem("qr.png"))),
                HttpStatus.BAD_REQUEST, "O campo 'grupo' é obrigatório");
    }

    @Test
    void deveRetornar400QuandoGrupoNaoPertenceAPaginaDaRota() {
        assertStatus(() -> paginaService.criarSecao(PAGINA_PIX, criar("telefone", "16999999999", null, null)),
                HttpStatus.BAD_REQUEST, "O grupo 'telefone' não é permitido na página 4.");
        assertStatus(() -> paginaService.criarSecao(PAGINA_TRANSPARENCIA, criar("pix", null, "chave", imagem("qr.png"))),
                HttpStatus.BAD_REQUEST, "O grupo 'pix' não é permitido na página 1.");
        assertThat(secoesGravadas).isEmpty();
    }

    @Test
    void deveRetornar400QuandoCampoObrigatorioEstaAusente() {
        assertStatus(() -> paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "   ", "1633333333", null)),
                HttpStatus.BAD_REQUEST, "O campo 'titulo' é obrigatório para o grupo 'telefone'.");
        assertStatus(() -> paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", null, null, imagem("a.png"))),
                HttpStatus.BAD_REQUEST, "O campo 'titulo' é obrigatório para o grupo 'produtos'.");
        assertStatus(() -> paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", "Folder A3", null, null)),
                HttpStatus.BAD_REQUEST, "O campo 'imagem' é obrigatório para o grupo 'produtos'.");
        assertStatus(() -> paginaService.criarSecao(PAGINA_PIX, criar("pix", null, null, imagem("qr.png"))),
                HttpStatus.BAD_REQUEST, "O campo 'conteudo' é obrigatório para o grupo 'pix'.");
        assertStatus(() -> paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave", null)),
                HttpStatus.BAD_REQUEST, "O campo 'imagem' é obrigatório para o grupo 'pix'.");

        assertThat(secoesGravadas).isEmpty();
        assertThat(arquivosNoStorage()).isEmpty();
    }

    @Test
    void deveRetornar400QuandoArquivoEhEnviadoParaGrupoSemImagem() {
        assertStatus(() -> paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", null, imagem("foto.png"))),
                HttpStatus.BAD_REQUEST, "O grupo 'telefone' não aceita arquivo de imagem.");

        assertThat(arquivosNoStorage()).isEmpty();
        verify(arquivoService, never()).salvarArquivo(any(), any(), any());
    }

    @Test
    void uploadAvulsoDeImagemTambemRespeitaOGrupo() {
        Secao telefone = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", null, null));

        assertStatus(() -> paginaService.atualizarImagemSecao(telefone.getId(), imagem("foto.png")),
                HttpStatus.BAD_REQUEST, "O grupo 'telefone' não aceita arquivo de imagem.");
        assertThat(arquivosNoStorage()).isEmpty();
    }

    @Test
    void paginasSemGruposDaGraficaOuPixMantemFluxoLivre() {
        Secao secao = paginaService.criarSecao(PAGINA_TRANSPARENCIA, criar("relatorios", "Relatórios Financeiros", "Texto", null));

        assertThat(secao.getGrupo()).isEqualTo("relatorios");
        assertThat(secao.getTitulo()).isEqualTo("Relatórios Financeiros");
    }

    // ---------------------------------------------------------------- PUT

    @Test
    void putDeveRetornar404ParaSecaoInexistenteOuDeOutraPagina() {
        Secao telefone = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", null, null));

        assertStatus(() -> paginaService.atualizarSecao(PAGINA_GRAFICA, 999L, atualizar("telefone", "16911111111", null, null)),
                HttpStatus.NOT_FOUND, "Seção não encontrada nesta página.");
        assertStatus(() -> paginaService.atualizarSecao(PAGINA_PIX, telefone.getId(), atualizar("telefone", "16911111111", null, null)),
                HttpStatus.NOT_FOUND, "Seção não encontrada nesta página.");
    }

    @Test
    void putNaoPermiteTrocarOGrupo() {
        Secao produto = paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", "Folder A3", null, imagem("a.png")));

        assertStatus(() -> paginaService.atualizarSecao(PAGINA_GRAFICA, produto.getId(), atualizar("telefone", "16999999999", null, null)),
                HttpStatus.BAD_REQUEST, "Não é permitido alterar o grupo da seção (grupo atual: 'produtos').");
        assertStatus(() -> paginaService.atualizarSecao(PAGINA_GRAFICA, produto.getId(), atualizar(null, "Folder A4", null, null)),
                HttpStatus.BAD_REQUEST, "O campo 'grupo' é obrigatório.");
        // A rota antiga (/paginas/secoes/{id}) passa pelas mesmas regras.
        assertStatus(() -> paginaService.atualizarSecao(produto.getId(), atualizar("telefone", "16999999999", null, null)),
                HttpStatus.BAD_REQUEST, "Não é permitido alterar o grupo da seção (grupo atual: 'produtos').");
    }

    @Test
    void putValidaCamposObrigatoriosEArquivoDoGrupo() {
        Secao telefone = paginaService.criarSecao(PAGINA_GRAFICA, criar("telefone", "16999999999", null, null));

        assertStatus(() -> paginaService.atualizarSecao(PAGINA_GRAFICA, telefone.getId(), atualizar("telefone", null, "1633333333", null)),
                HttpStatus.BAD_REQUEST, "O campo 'titulo' é obrigatório para o grupo 'telefone'.");
        assertStatus(() -> paginaService.atualizarSecao(PAGINA_GRAFICA, telefone.getId(), atualizar("telefone", "16999999999", null, imagem("x.png"))),
                HttpStatus.BAD_REQUEST, "O grupo 'telefone' não aceita arquivo de imagem.");
        assertThat(arquivosNoStorage()).isEmpty();
    }

    @Test
    void putSemArquivoMantemImagemAtualEAnulaCamposQueOGrupoNaoUsa() {
        Secao produto = paginaService.criarSecao(PAGINA_GRAFICA, criar("produtos", "Folder A3", null, imagem("a.png")));
        String imagemAtual = produto.getImagem();

        Secao atualizado = emTransacao(true, () -> paginaService.atualizarSecao(PAGINA_GRAFICA, produto.getId(),
                atualizar("PRODUTOS", "Folder A4", "conteúdo ignorado", null)));

        assertThat(atualizado.getTitulo()).isEqualTo("Folder A4");
        assertThat(atualizado.getConteudo()).isNull();
        assertThat(atualizado.getImagem()).isEqualTo(imagemAtual);
        assertThat(arquivoNoDisco(imagemAtual)).exists();
    }

    @Test
    void putComRollbackRemoveOArquivoNovoEMantemOAntigo() {
        Secao pix = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave", imagem("qr-antigo.png")));
        String qrAntigo = pix.getImagem();

        Secao atualizado = emTransacao(false, () -> paginaService.atualizarSecao(PAGINA_PIX, pix.getId(),
                atualizar("pix", null, "outra-chave", imagem("qr-novo.png"))));

        assertThat(arquivoNoDisco(atualizado.getImagem())).doesNotExist();
        assertThat(arquivoNoDisco(qrAntigo)).exists();
    }

    @Test
    void putComFalhaNaPersistenciaRemoveOArquivoNovoEPropagaOErro() {
        Secao pix = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave", imagem("qr-antigo.png")));
        String qrAntigo = pix.getImagem();
        doThrow(new DataIntegrityViolationException("falha simulada")).when(secaoRepository).save(any(Secao.class));

        assertThatThrownBy(() -> emTransacao(true, () -> paginaService.atualizarSecao(PAGINA_PIX, pix.getId(),
                atualizar("pix", null, "outra-chave", imagem("qr-novo.png")))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(arquivosNoStorage()).containsExactly(arquivoNoDisco(qrAntigo));
    }

    @Test
    void falhaAoApagarArquivoAntigoNaoQuebraARequisicao() {
        Secao pix = paginaService.criarSecao(PAGINA_PIX, criar("pix", null, "chave", imagem("qr-antigo.png")));
        String qrAntigo = pix.getImagem();
        doThrow(new IllegalStateException("storage indisponível")).when(arquivoService).deletarArquivo(qrAntigo);

        Secao atualizado = emTransacao(true, () -> paginaService.atualizarSecao(PAGINA_PIX, pix.getId(),
                atualizar("pix", null, "outra-chave", imagem("qr-novo.png"))));

        assertThat(atualizado.getConteudo()).isEqualTo("outra-chave");
        assertThat(arquivoNoDisco(atualizado.getImagem())).exists();
        verify(arquivoService).deletarArquivo(qrAntigo);
    }

    // ---------------------------------------------------------------- utilitários

    /**
     * Abre uma "transação" só com o mecanismo de sincronização do Spring e, ao final,
     * dispara afterCompletion como faria o gerenciador real (commit ou rollback).
     */
    private <T> T emTransacao(boolean confirmar, Supplier<T> operacao) {
        TransactionSynchronizationManager.initSynchronization();
        int status = confirmar ? TransactionSynchronization.STATUS_COMMITTED : TransactionSynchronization.STATUS_ROLLED_BACK;
        try {
            return operacao.get();
        } catch (RuntimeException excecao) {
            status = TransactionSynchronization.STATUS_ROLLED_BACK;
            throw excecao;
        } finally {
            List<TransactionSynchronization> sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            for (TransactionSynchronization sincronizacao : sincronizacoes) {
                sincronizacao.afterCompletion(status);
            }
        }
    }

    private void assertStatus(Runnable operacao, HttpStatus status, String mensagem) {
        assertThatThrownBy(operacao::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(excecao -> {
                    ResponseStatusException erro = (ResponseStatusException) excecao;
                    assertThat(erro.getStatusCode()).isEqualTo(status);
                    assertThat(erro.getReason()).contains(mensagem);
                });
    }

    private Path arquivoNoDisco(String caminhoNoBanco) {
        return diretorioUploads.resolve(caminhoNoBanco.substring("/uploads/".length()));
    }

    private List<Path> arquivosNoStorage() {
        try (Stream<Path> arquivos = Files.walk(diretorioUploads)) {
            return arquivos.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Pagina pagina(Long id, String nome) {
        Pagina pagina = new Pagina();
        pagina.setId(id);
        pagina.setNome(nome);
        pagina.setAtivo(true);
        return pagina;
    }

    private static CriarSecaoDTO criar(String grupo, String titulo, String conteudo, MultipartFile imagem) {
        CriarSecaoDTO dto = new CriarSecaoDTO();
        dto.setGrupo(grupo);
        dto.setTitulo(titulo);
        dto.setConteudo(conteudo);
        dto.setImagem(imagem);
        return dto;
    }

    private static AtualizarSecaoDTO atualizar(String grupo, String titulo, String conteudo, MultipartFile imagem) {
        AtualizarSecaoDTO dto = new AtualizarSecaoDTO();
        dto.setGrupo(grupo);
        dto.setTitulo(titulo);
        dto.setConteudo(conteudo);
        dto.setImagem(imagem);
        return dto;
    }

    private static MultipartFile imagem(String nome) {
        return new MockMultipartFile("imagem", nome, "image/png", ("png-" + nome).getBytes());
    }
}
