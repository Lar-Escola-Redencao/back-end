package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.dto.DocumentoResponseDTO;
import br.org.larescolaredencao.dto.ReordenarSecaoDTO;
import br.org.larescolaredencao.model.Documento;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.DocumentoRepository;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;
import jakarta.transaction.Transactional;

/**
 * Regras do CMS de páginas institucionais. É agnóstico de qual página está sendo
 * editada: toda operação parte do id da página, que precisa estar pré-cadastrada.
 */
@Service
public class PaginaService {

    private static final Logger logger = LoggerFactory.getLogger(PaginaService.class);

    private static final String GRUPO_TEXTO_SOBRE = "texto-sobre";

    private static final Long ID_PAGINA_GRAFICA = 3L;
    private static final Long ID_PAGINA_PIX = 4L;
    private static final String GRUPO_TELEFONE = "telefone";
    private static final String GRUPO_PRODUTOS = "produtos";
    private static final String GRUPO_PIX = "pix";
    private static final int TITULO_TAMANHO_MINIMO = 3;
    private static final int TITULO_TAMANHO_MAXIMO = 150;
    private static final String TITULO_TEXTO_SOBRE = "Sobre o Lar Escola Redenção";

    private final PaginaRepository paginaRepository;
    private final SecaoRepository secaoRepository;
    private final DocumentoRepository documentoRepository;
    private final ArquivoService arquivoService;

    public PaginaService(PaginaRepository paginaRepository,
                         SecaoRepository secaoRepository,
                         DocumentoRepository documentoRepository,
                         ArquivoService arquivoService) {
        this.paginaRepository = paginaRepository;
        this.secaoRepository = secaoRepository;
        this.documentoRepository = documentoRepository;
        this.arquivoService = arquivoService;
    }

    public Pagina buscarPaginaPorId(Long id) {
        return paginaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Página não encontrada."));
    }

    public List<Secao> listarSecoes(Long idPagina) {
        buscarPaginaPorId(idPagina);
        return secaoRepository.findByPaginaIdOrderByGrupoAscOrdemAsc(idPagina);
    }

    public Page<Secao> listarSecoesPaginado(Long idPagina, Pageable pageable, String grupo) {
        if (grupo == null || grupo.isBlank()) {
            return secaoRepository.findByPaginaId(idPagina, pageable);
        }
        return secaoRepository.findByPaginaIdAndGrupo(idPagina, grupo, pageable);
    }

    public Page<Secao> listarSecoesPaginado(Long idPagina, Pageable pageable) {
        return listarSecoesPaginado(idPagina, pageable, null);
    }

    public Page<DocumentoResponseDTO> listarDocumentosPaginado(Long idPagina, Pageable pageable) {
        return documentoRepository.findBySecaoPaginaId(idPagina, pageable)
                .map(DocumentoResponseDTO::new);
    }

    public Secao buscarSecaoPorId(Long id) {
        return secaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada."));
    }

    /**
     * Gráfica (telefone, produtos) e Pix (pix) gravam cada informação numa coluna
     * definida pelo grupo. As demais páginas mantêm o comportamento livre de antes.
     */
    @Transactional
    public Secao criarSecao(Long idPagina, CriarSecaoDTO dto) {
        Pagina pagina = buscarPaginaPorId(idPagina);

        String grupoDaPagina = validarGrupoDaPagina(idPagina, dto.getGrupo());
        if (grupoDaPagina != null) {
            return criarSecaoDeGrupo(pagina, grupoDaPagina, dto);
        }

        validarTamanhoTitulo(dto.getTitulo());
        Secao secao = new Secao();
        String grupo = normalizarGrupo(dto.getGrupo());
        secao.setTitulo(resolverTituloSecao(dto.getTitulo(), grupo));
        secao.setConteudo(normalizarConteudo(dto.getConteudo()));
        secao.setGrupo(grupo);
        secao.setOrdem(dto.getOrdem() == null ? proximaOrdem(idPagina, grupo) : dto.getOrdem());
        secao.setAtivo(true);
        secao.setPagina(pagina);

        String novaImagem = null;
        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            novaImagem = arquivoService.salvarArquivo(dto.getImagem(), subPastaImagens(idPagina), TipoArquivo.FOTO);
            secao.setImagem(novaImagem);
        }

        return persistirSecao(secao, novaImagem, null);
    }

    /**
     * Rota antiga (/paginas/secoes/{id}): resolve a página da seção e aplica as mesmas regras.
     * Busca só o id da página para a seção ser carregada uma única vez, já com lock.
     */
    @Transactional
    public Secao atualizarSecao(Long id, AtualizarSecaoDTO dto) {
        Long idPagina = secaoRepository.findIdPaginaById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada."));
        return atualizarSecao(idPagina, id, dto);
    }

    @Transactional
    public Secao atualizarSecao(Long idPagina, Long id, AtualizarSecaoDTO dto) {
        buscarPaginaPorId(idPagina);
        Secao secao = secaoRepository.findByIdAndPaginaId(id, idPagina)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada nesta página."));

        if (ehGrupoGraficaOuPix(normalizarChaveGrupo(secao.getGrupo())) || !gruposDaPagina(idPagina).isEmpty()) {
            return atualizarSecaoDeGrupo(secao, dto);
        }

        if (dto.getTitulo() != null) {
            validarTamanhoTitulo(dto.getTitulo());
            secao.setTitulo(arquivoService.sanitizarTexto(dto.getTitulo()));
        }
        if (dto.getConteudo() != null) {
            secao.setConteudo(normalizarConteudo(dto.getConteudo()));
        }
        if (dto.getAtivo() != null) {
            secao.setAtivo(dto.getAtivo());
        }
        if (dto.getGrupo() != null) {
            // Impede que uma seção livre vire um grupo da Gráfica/Pix (ex.: telefone na Transparência).
            validarGrupoDaPagina(idPagina, dto.getGrupo());
            secao.setGrupo(normalizarGrupo(dto.getGrupo()));
        }
        if (dto.getOrdem() != null) {
            secao.setOrdem(dto.getOrdem());
        }

        String novaImagem = null;
        String imagemAnterior = null;
        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            imagemAnterior = secao.getImagem();
            novaImagem = arquivoService.salvarArquivo(dto.getImagem(), subPastaImagens(idPagina), TipoArquivo.FOTO);
            secao.setImagem(novaImagem);
        }
        return persistirSecao(secao, novaImagem, imagemAnterior);
    }

    @Transactional
    public void reordenarSecoes(List<ReordenarSecaoDTO> secoes) {
        for (ReordenarSecaoDTO item : secoes) {
            Secao secao = buscarSecaoPorId(item.getId());
            secao.setOrdem(item.getOrdem());
            secaoRepository.save(secao);
        }
    }

    /** Upload genérico de imagem: grava a nova e remove fisicamente a anterior, quando havia uma. */
    @Transactional
    public Secao atualizarImagemSecao(Long id, MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem é obrigatória.");
        }

        Secao secao = secaoRepository.findByIdComBloqueio(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada."));
        validarArquivoPermitido(normalizarChaveGrupo(secao.getGrupo()), imagem);
        String imagemAnterior = secao.getImagem();

        String novaImagem = arquivoService.salvarArquivo(imagem,
                subPastaImagens(secao.getPagina().getId()), TipoArquivo.FOTO);
        secao.setImagem(novaImagem);

        return persistirSecao(secao, novaImagem, imagemAnterior);
    }

    public void deletarSecao(Long id) {
        Secao secao = buscarSecaoPorId(id);
        arquivoService.deletarArquivo(secao.getImagem());
        for (Documento documento : secao.getDocumentos()) {
            arquivoService.deletarArquivo(documento.getArquivo());
        }
        secaoRepository.delete(secao);
    }

    public Documento adicionarDocumento(Long secaoId, String titulo, MultipartFile arquivo) {
        String tituloSanitizado = arquivoService.sanitizarTexto(titulo);
        if (tituloSanitizado == null || tituloSanitizado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título do documento é obrigatório.");
        }

        Secao secao = buscarSecaoPorId(secaoId);

        String caminho = arquivoService.salvarArquivo(arquivo,
                subPastaDocumentos(secao.getPagina().getId()), TipoArquivo.DOCUMENTO);

        Documento documento = new Documento();
        documento.setTitulo(tituloSanitizado);
        documento.setArquivo(caminho);
        documento.setSecao(secao);

        return documentoRepository.save(documento);
    }

    public Documento buscarDocumentoPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
    }

    public Documento atualizarDocumento(Long id, Long secaoId, String titulo, MultipartFile arquivo) {
        Documento documento = buscarDocumentoPorId(id);

        String tituloSanitizado = arquivoService.sanitizarTexto(titulo);
        if (tituloSanitizado != null && !tituloSanitizado.isEmpty()) {
            documento.setTitulo(tituloSanitizado);
        }

        if (secaoId != null && !secaoId.equals(documento.getSecao().getId())) {
            Secao novaSecao = buscarSecaoPorId(secaoId);
            documento.setSecao(novaSecao);
        }

        if (arquivo != null && !arquivo.isEmpty()) {
            String caminhoAnterior = documento.getArquivo();
            String novoCaminho = arquivoService.salvarArquivo(arquivo,
                    subPastaDocumentos(documento.getSecao().getPagina().getId()), TipoArquivo.DOCUMENTO);

            documento.setArquivo(novoCaminho);
            arquivoService.deletarArquivo(caminhoAnterior);
        }

        return documentoRepository.save(documento);
    }

    public void deletarDocumento(Long id) {
        Documento documento = buscarDocumentoPorId(id);
        arquivoService.deletarArquivo(documento.getArquivo());
        documentoRepository.delete(documento);
    }

    /**
     * Gráfica e Pix só aceitam os próprios grupos, e esses grupos não podem ser usados em
     * outras páginas. Devolve o grupo normalizado, ou null quando a página segue o fluxo
     * livre (Transparência/Sobre).
     */
    private String validarGrupoDaPagina(Long idPagina, String grupoInformado) {
        String grupo = normalizarChaveGrupo(grupoInformado);
        List<String> gruposAceitos = gruposDaPagina(idPagina);

        if (gruposAceitos.contains(grupo)) {
            return grupo;
        }
        if (ehGrupoGraficaOuPix(grupo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O grupo '" + grupo + "' não é permitido na página " + idPagina + ".");
        }
        if (gruposAceitos.isEmpty()) {
            return null;
        }
        if (grupo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O campo 'grupo' é obrigatório. Grupos aceitos nesta página: " + String.join(", ", gruposAceitos) + ".");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Grupo '" + grupo + "' inválido para esta página. Grupos aceitos: " + String.join(", ", gruposAceitos) + ".");
    }

    private List<String> gruposDaPagina(Long idPagina) {
        if (ID_PAGINA_GRAFICA.equals(idPagina)) {
            return List.of(GRUPO_TELEFONE, GRUPO_PRODUTOS);
        }
        if (ID_PAGINA_PIX.equals(idPagina)) {
            return List.of(GRUPO_PIX);
        }
        return List.of();
    }

    private boolean ehGrupoGraficaOuPix(String grupo) {
        return GRUPO_TELEFONE.equals(grupo) || GRUPO_PRODUTOS.equals(grupo) || GRUPO_PIX.equals(grupo);
    }

    /** Telefone e pix têm um único registro por página; produtos aceita vários. */
    private boolean ehRegistroUnico(String grupo) {
        return GRUPO_TELEFONE.equals(grupo) || GRUPO_PIX.equals(grupo);
    }

    /** Telefone e pix viram upsert por (página, grupo); produtos sempre cria um registro novo. */
    private Secao criarSecaoDeGrupo(Pagina pagina, String grupo, CriarSecaoDTO dto) {
        Secao secao = null;
        if (ehRegistroUnico(grupo)) {
            // O lock da página serializa os upserts concorrentes; a busca seguinte também é com lock.
            paginaRepository.findByIdComBloqueio(pagina.getId());
            secao = secaoRepository.findFirstByPaginaIdAndGrupo(pagina.getId(), grupo).orElse(null);
        }

        if (secao == null) {
            secao = new Secao();
            secao.setPagina(pagina);
            secao.setGrupo(grupo);
            secao.setAtivo(true);
            secao.setOrdem(dto.getOrdem() == null ? proximaOrdem(pagina.getId(), grupo) : dto.getOrdem());
        } else if (dto.getOrdem() != null) {
            secao.setOrdem(dto.getOrdem());
        }

        return gravarCamposDoGrupo(secao, grupo, dto.getTitulo(), dto.getConteudo(), dto.getImagem());
    }

    /** O grupo do registro é fixo: o PUT precisa repetir o mesmo grupo, sem permitir a troca. */
    private Secao atualizarSecaoDeGrupo(Secao secao, AtualizarSecaoDTO dto) {
        if (dto.getGrupo() == null || dto.getGrupo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo 'grupo' é obrigatório.");
        }

        String grupo = validarGrupoDaPagina(secao.getPagina().getId(), dto.getGrupo());
        if (grupo == null || !grupo.equals(normalizarChaveGrupo(secao.getGrupo()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é permitido alterar o grupo da seção (grupo atual: '" + secao.getGrupo() + "').");
        }

        if (dto.getAtivo() != null) {
            secao.setAtivo(dto.getAtivo());
        }
        if (dto.getOrdem() != null) {
            secao.setOrdem(dto.getOrdem());
        }

        return gravarCamposDoGrupo(secao, grupo, dto.getTitulo(), dto.getConteudo(), dto.getImagem());
    }

    /**
     * Grava cada informação na coluna definida pelo grupo; colunas que o grupo não usa
     * ficam NULL. Toda validação acontece antes do upload, para não gravar arquivo de
     * requisição inválida.
     */
    private Secao gravarCamposDoGrupo(Secao secao, String grupo, String titulo, String conteudo,
                                      MultipartFile arquivo) {
        boolean possuiArquivo = arquivo != null && !arquivo.isEmpty();
        validarArquivoPermitido(grupo, arquivo);
        String imagemAnterior = secao.getImagem();

        switch (grupo) {
            case GRUPO_TELEFONE -> {
                // WhatsApp em titulo, telefone alternativo (opcional) em conteudo, sem imagem.
                secao.setTitulo(validarTamanhoTitulo(textoObrigatorio(grupo, "titulo", arquivoService.sanitizarTexto(titulo))));
                secao.setConteudo(textoOuNulo(conteudo));
                secao.setImagem(null);
            }
            case GRUPO_PRODUTOS -> {
                // Nome do produto em titulo e foto em imagem; conteudo não é usado.
                secao.setTitulo(validarTamanhoTitulo(textoObrigatorio(grupo, "titulo", arquivoService.sanitizarTexto(titulo))));
                secao.setConteudo(null);
                exigirImagem(grupo, possuiArquivo, imagemAnterior);
            }
            case GRUPO_PIX -> {
                // Chave em conteudo e QR Code em imagem; titulo não é usado.
                secao.setTitulo(null);
                secao.setConteudo(textoObrigatorio(grupo, "conteudo", conteudo));
                exigirImagem(grupo, possuiArquivo, imagemAnterior);
            }
            default -> throw new IllegalStateException("Grupo sem regra de gravação: " + grupo);
        }

        String novaImagem = null;
        if (possuiArquivo) {
            novaImagem = arquivoService.salvarArquivo(arquivo, subPastaImagens(secao.getPagina().getId()), TipoArquivo.FOTO);
            secao.setImagem(novaImagem);
        }

        String imagemDescartada = imagemAnterior != null && !imagemAnterior.equals(secao.getImagem()) ? imagemAnterior : null;
        return persistirSecao(secao, novaImagem, imagemDescartada);
    }

    private void validarArquivoPermitido(String grupo, MultipartFile arquivo) {
        if (arquivo != null && !arquivo.isEmpty() && GRUPO_TELEFONE.equals(grupo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O grupo '" + grupo + "' não aceita arquivo de imagem.");
        }
    }

    /** A imagem só é obrigatória na criação: com uma já gravada, o arquivo novo é opcional. */
    private void exigirImagem(String grupo, boolean possuiArquivo, String imagemAtual) {
        if (!possuiArquivo && imagemAtual == null) {
            throw campoObrigatorio(grupo, "imagem");
        }
    }

    private String textoObrigatorio(String grupo, String campo, String valor) {
        String texto = textoOuNulo(valor);
        if (texto == null) {
            throw campoObrigatorio(grupo, campo);
        }
        return texto;
    }

    /**
     * Limite de tamanho do título, aplicado só onde o título é usado. Antes ficava no
     * @Size dos DTOs, que rodava antes do mapeamento e recusava o pix com titulo="".
     */
    private String validarTamanhoTitulo(String titulo) {
        if (titulo != null && (titulo.length() < TITULO_TAMANHO_MINIMO || titulo.length() > TITULO_TAMANHO_MAXIMO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O campo 'titulo' deve ter entre " + TITULO_TAMANHO_MINIMO + " e " + TITULO_TAMANHO_MAXIMO + " caracteres.");
        }
        return titulo;
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ResponseStatusException campoObrigatorio(String grupo, String campo) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O campo '" + campo + "' é obrigatório para o grupo '" + grupo + "'.");
    }

    /**
     * Persiste a seção sem deixar arquivo órfão: se a gravação falhar, o arquivo recém-enviado
     * é removido; o arquivo descartado só sai do storage depois do commit.
     */
    private Secao persistirSecao(Secao secao, String arquivoNovo, String arquivoDescartado) {
        Secao secaoSalva;
        try {
            secaoSalva = secaoRepository.save(secao);
            secaoRepository.flush();
        } catch (RuntimeException excecao) {
            removerArquivoDoStorage(arquivoNovo);
            throw excecao;
        }
        agendarRemocaoDeArquivos(arquivoNovo, arquivoDescartado);
        return secaoSalva;
    }

    /** Commit confirmado: remove o arquivo descartado. Rollback: remove o arquivo novo. */
    private void agendarRemocaoDeArquivos(String arquivoNovo, String arquivoDescartado) {
        if (arquivoNovo == null && arquivoDescartado == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removerArquivoDoStorage(arquivoDescartado);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                removerArquivoDoStorage(status == STATUS_COMMITTED ? arquivoDescartado : arquivoNovo);
            }
        });
    }

    /** Falha ao apagar arquivo não pode derrubar a requisição: só registra no log. */
    private void removerArquivoDoStorage(String caminho) {
        try {
            arquivoService.deletarArquivo(caminho);
        } catch (RuntimeException excecao) {
            logger.warn("Falha ao remover o arquivo {} do storage.", caminho, excecao);
        }
    }

    /**
     * O front envia string vazia quando a aba não tem texto; não faz sentido gravar "" no banco.
     */
    private String normalizarConteudo(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            return null;
        }
        return conteudo;
    }

    private String normalizarGrupo(String grupo) {
        if (grupo == null || grupo.isBlank()) {
            return "nenhum";
        }
        return grupo.trim();
    }

    /** Trim + minúsculas, para que " Telefone " e "telefone" sejam o mesmo grupo. */
    private String normalizarChaveGrupo(String grupo) {
        return grupo == null ? "" : grupo.trim().toLowerCase(Locale.ROOT);
    }

    private String resolverTituloSecao(String titulo, String grupo) {
        String tituloSanitizado = arquivoService.sanitizarTexto(titulo);
        if (tituloSanitizado != null && !tituloSanitizado.isBlank()) {
            return tituloSanitizado;
        }
        if (GRUPO_TEXTO_SOBRE.equals(grupo)) {
            return TITULO_TEXTO_SOBRE;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título da seção é obrigatório.");
    }

    private Integer proximaOrdem(Long idPagina, String grupo) {
        Integer maiorOrdem = secaoRepository.findMaxOrdemByPaginaIdAndGrupo(idPagina, grupo);
        return maiorOrdem == null ? 1 : maiorOrdem + 1;
    }

    private String subPastaImagens(Long idPagina) {
        return "paginas/" + idPagina + "/imagens/";
    }

    private String subPastaDocumentos(Long idPagina) {
        return "paginas/" + idPagina + "/documentos/";
    }
}
