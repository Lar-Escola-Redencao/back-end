package br.org.larescolaredencao.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.org.larescolaredencao.dto.AtualizarSecaoDTO;
import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.dto.DocumentoResponseDTO;
import br.org.larescolaredencao.model.Documento;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.service.ArquivoService;
import br.org.larescolaredencao.service.PaginaService;
import jakarta.validation.Valid;

/**
 * CMS genérico de páginas institucionais. As rotas de listagem e de criação recebem
 * o id da página; seção e documento são endereçados pelo próprio id, que já é único.
 */
@RestController
@RequestMapping("/paginas")
public class PaginaController {

    private final PaginaService paginaService;
    private final ArquivoService arquivoService;

    public PaginaController(PaginaService paginaService, ArquivoService arquivoService) {
        this.paginaService = paginaService;
        this.arquivoService = arquivoService;
    }

    /** O padrão numérico evita ambiguidade com as rotas literais /paginas/secoes e /paginas/documentos. */
    @GetMapping("/{idPagina:\\d+}")
    public Pagina obterPagina(@PathVariable("idPagina") Long idPagina) {
        return paginaService.buscarPaginaPorId(idPagina);
    }

    @GetMapping("/{idPagina:\\d+}/secoes")
    public List<Secao> listarSecoes(@PathVariable("idPagina") Long idPagina) {
        return paginaService.listarSecoes(idPagina);
    }

    @GetMapping("/{idPagina:\\d+}/secoes/admin")
    public PagedModel<Secao> listarSecoesAdmin(@PathVariable("idPagina") Long idPagina, Pageable pageable) {
        return new PagedModel<>(paginaService.listarSecoesPaginado(idPagina, pageable));
    }

    @GetMapping("/{idPagina:\\d+}/documentos/admin")
    public PagedModel<DocumentoResponseDTO> listarDocumentosAdmin(@PathVariable("idPagina") Long idPagina,
                                                                  Pageable pageable) {
        return new PagedModel<>(paginaService.listarDocumentosPaginado(idPagina, pageable));
    }

    @PostMapping("/{idPagina:\\d+}/secoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Secao criarSecao(@PathVariable("idPagina") Long idPagina, @Valid @ModelAttribute CriarSecaoDTO dto) {
        return paginaService.criarSecao(idPagina, dto);
    }

    @GetMapping("/secoes/{id}")
    public Secao buscarSecao(@PathVariable("id") Long id) {
        return paginaService.buscarSecaoPorId(id);
    }

    @PutMapping("/secoes/{id}")
    public Secao atualizarSecao(@PathVariable("id") Long id, @Valid @ModelAttribute AtualizarSecaoDTO dto) {
        return paginaService.atualizarSecao(id, dto);
    }

    @PutMapping("/secoes/{id}/imagem")
    public Secao atualizarImagemSecao(@PathVariable("id") Long id,
                                      @RequestParam("imagem") MultipartFile imagem) {
        return paginaService.atualizarImagemSecao(id, imagem);
    }

    @DeleteMapping("/secoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarSecao(@PathVariable("id") Long id) {
        paginaService.deletarSecao(id);
    }

    @PostMapping("/secoes/{secaoId}/documentos")
    public Documento adicionarDocumento(@PathVariable("secaoId") Long secaoId,
                                        @RequestParam("titulo") String titulo,
                                        @RequestParam("arquivo") MultipartFile arquivo) {
        return paginaService.adicionarDocumento(secaoId, titulo, arquivo);
    }

    @PutMapping("/documentos/{id}")
    public Documento atualizarDocumento(@PathVariable("id") Long id,
                                        @RequestParam("secaoId") Long secaoId,
                                        @RequestParam("titulo") String titulo,
                                        @RequestParam(value = "arquivo", required = false) MultipartFile arquivo) {
        return paginaService.atualizarDocumento(id, secaoId, titulo, arquivo);
    }

    @GetMapping("/documentos/{id}/download")
    public ResponseEntity<Resource> baixarDocumento(@PathVariable("id") Long id) {
        Documento documento = paginaService.buscarDocumentoPorId(id);
        Resource recurso = arquivoService.carregarComoRecurso(documento.getArquivo());

        String extensao = arquivoService.extrairExtensao(documento.getArquivo());
        String nomeParaDownload = extensao.isEmpty()
                ? documento.getTitulo()
                : documento.getTitulo() + "." + extensao;

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(nomeParaDownload, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(recurso);
    }

    @DeleteMapping("/documentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarDocumento(@PathVariable("id") Long id) {
        paginaService.deletarDocumento(id);
    }
}
