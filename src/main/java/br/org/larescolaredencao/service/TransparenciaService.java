package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.model.Documento;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.repository.DocumentoRepository;
import br.org.larescolaredencao.repository.PaginaRepository;
import br.org.larescolaredencao.repository.SecaoRepository;

@Service
public class TransparenciaService {

    private static final String NOME_PAGINA = "Transparência";
    private static final String SUBPASTA_DOCUMENTOS = "transparencia/documentos/";
    private static final String SUBPASTA_IMAGENS = "transparencia/imagens/";

    private final PaginaRepository paginaRepository;
    private final SecaoRepository secaoRepository;
    private final DocumentoRepository documentoRepository;
    private final ArquivoService arquivoService;

    public TransparenciaService(PaginaRepository paginaRepository,
                                 SecaoRepository secaoRepository,
                                 DocumentoRepository documentoRepository,
                                 ArquivoService arquivoService) {
        this.paginaRepository = paginaRepository;
        this.secaoRepository = secaoRepository;
        this.documentoRepository = documentoRepository;
        this.arquivoService = arquivoService;
    }

    public Pagina obterPaginaTransparencia() {
        return paginaRepository.findByNome(NOME_PAGINA)
                .orElseGet(() -> {
                    Pagina pagina = new Pagina();
                    pagina.setNome(NOME_PAGINA);
                    pagina.setAtivo(true);
                    return paginaRepository.save(pagina);
                });
    }

    public List<Secao> listarSecoes() {
        return obterPaginaTransparencia().getSecoes();
    }

    public Secao buscarSecaoPorId(Long id) {
        return secaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seção não encontrada."));
    }

    public Secao criarSecao(CriarSecaoDTO dto) {
        Secao secao = new Secao();
        secao.setTitulo(arquivoService.sanitizarTexto(dto.getTitulo()));
        secao.setConteudo(dto.getConteudo());
        secao.setAtivo(true);
        secao.setPagina(obterPaginaTransparencia());

        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            secao.setImagem(arquivoService.salvarArquivo(dto.getImagem(), SUBPASTA_IMAGENS, TipoArquivo.FOTO));
        }

        return secaoRepository.save(secao);
    }

    public Secao atualizarSecao(Long id, AtualizarSecaoDTO dto) {
        Secao secao = buscarSecaoPorId(id);
        if (dto.getTitulo() != null) {
            secao.setTitulo(arquivoService.sanitizarTexto(dto.getTitulo()));
        }
        if (dto.getConteudo() != null) {
            secao.setConteudo(dto.getConteudo());
        }
        if (dto.getAtivo() != null) {
            secao.setAtivo(dto.getAtivo());
        }
        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            String imagemAnterior = secao.getImagem();
            secao.setImagem(arquivoService.salvarArquivo(dto.getImagem(), SUBPASTA_IMAGENS, TipoArquivo.FOTO));
            arquivoService.deletarArquivo(imagemAnterior);
        }
        return secaoRepository.save(secao);
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

        String caminho = arquivoService.salvarArquivo(arquivo, SUBPASTA_DOCUMENTOS, TipoArquivo.DOCUMENTO);

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
            String novoCaminho = arquivoService.salvarArquivo(arquivo, SUBPASTA_DOCUMENTOS, TipoArquivo.DOCUMENTO);
            
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
}
