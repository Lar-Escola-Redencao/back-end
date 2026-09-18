package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.dto.DocumentoResponseDTO;
import br.org.larescolaredencao.model.Documento;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.DocumentoRepository;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

/**
 * Regras do CMS de páginas institucionais. É agnóstico de qual página está sendo
 * editada: toda operação parte do id da página, que precisa estar pré-cadastrada.
 */
@Service
public class PaginaService {

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
        return secaoRepository.findByPaginaIdOrderByIdAsc(idPagina);
    }

    public Page<Secao> listarSecoesPaginado(Long idPagina, Pageable pageable) {
        return secaoRepository.findByPaginaId(idPagina, pageable);
    }

    public Page<DocumentoResponseDTO> listarDocumentosPaginado(Long idPagina, Pageable pageable) {
        return documentoRepository.findBySecaoPaginaId(idPagina, pageable)
                .map(DocumentoResponseDTO::new);
    }

    public Secao buscarSecaoPorId(Long id) {
        return secaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada."));
    }

    public Secao criarSecao(Long idPagina, CriarSecaoDTO dto) {
        Pagina pagina = buscarPaginaPorId(idPagina);

        Secao secao = new Secao();
        secao.setTitulo(arquivoService.sanitizarTexto(dto.getTitulo()));
        secao.setConteudo(normalizarConteudo(dto.getConteudo()));
        secao.setAtivo(true);
        secao.setPagina(pagina);

        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            secao.setImagem(arquivoService.salvarArquivo(dto.getImagem(), subPastaImagens(idPagina), TipoArquivo.FOTO));
        }

        return secaoRepository.save(secao);
    }

    public Secao atualizarSecao(Long id, AtualizarSecaoDTO dto) {
        Secao secao = buscarSecaoPorId(id);
        if (dto.getTitulo() != null) {
            secao.setTitulo(arquivoService.sanitizarTexto(dto.getTitulo()));
        }
        if (dto.getConteudo() != null) {
            secao.setConteudo(normalizarConteudo(dto.getConteudo()));
        }
        if (dto.getAtivo() != null) {
            secao.setAtivo(dto.getAtivo());
        }
        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            String imagemAnterior = secao.getImagem();
            secao.setImagem(arquivoService.salvarArquivo(dto.getImagem(),
                    subPastaImagens(secao.getPagina().getId()), TipoArquivo.FOTO));
            arquivoService.deletarArquivo(imagemAnterior);
        }
        return secaoRepository.save(secao);
    }

    /** Upload genérico de imagem: grava a nova e remove fisicamente a anterior, quando havia uma. */
    public Secao atualizarImagemSecao(Long id, MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem é obrigatória.");
        }

        Secao secao = buscarSecaoPorId(id);
        String imagemAnterior = secao.getImagem();

        secao.setImagem(arquivoService.salvarArquivo(imagem,
                subPastaImagens(secao.getPagina().getId()), TipoArquivo.FOTO));
        Secao secaoSalva = secaoRepository.save(secao);

        arquivoService.deletarArquivo(imagemAnterior);

        return secaoSalva;
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
     * O front envia string vazia quando a aba não tem texto; não faz sentido gravar "" no banco.
     */
    private String normalizarConteudo(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            return null;
        }
        return conteudo;
    }

    private String subPastaImagens(Long idPagina) {
        return "paginas/" + idPagina + "/imagens/";
    }

    private String subPastaDocumentos(Long idPagina) {
        return "paginas/" + idPagina + "/documentos/";
    }
}
