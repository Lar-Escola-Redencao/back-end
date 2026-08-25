package br.org.larescolaredencao.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
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
import br.org.larescolaredencao.model.Documento;
import br.org.larescolaredencao.model.Pagina;
import br.org.larescolaredencao.model.Secao;
import br.org.larescolaredencao.service.ArquivoService;
import br.org.larescolaredencao.service.TransparenciaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/transparencia")
public class TransparenciaController {

    private final TransparenciaService transparenciaService;
    private final ArquivoService arquivoService;

    public TransparenciaController(TransparenciaService transparenciaService, ArquivoService arquivoService) {
        this.transparenciaService = transparenciaService;
        this.arquivoService = arquivoService;
    }

    @GetMapping
    public Pagina obterPagina() {
        return transparenciaService.obterPaginaTransparencia();
    }

    @GetMapping("/secoes")
    public List<Secao> listarSecoes() {
        return transparenciaService.listarSecoes();
    }

    @GetMapping("/secoes/{id}")
    public Secao buscarSecao(@PathVariable Long id) {
        return transparenciaService.buscarSecaoPorId(id);
    }

    @PostMapping("/secoes")
    public Secao criarSecao(@Valid @ModelAttribute CriarSecaoDTO dto) {
        return transparenciaService.criarSecao(dto);
    }

    @PutMapping("/secoes/{id}")
    public Secao atualizarSecao(@PathVariable Long id, @Valid @ModelAttribute AtualizarSecaoDTO dto) {
        return transparenciaService.atualizarSecao(id, dto);
    }

    @DeleteMapping("/secoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarSecao(@PathVariable Long id) {
        transparenciaService.deletarSecao(id);
    }

    @PostMapping("/secoes/{secaoId}/documentos")
    public Documento adicionarDocumento(@PathVariable Long secaoId,
                                         @RequestParam("titulo") String titulo,
                                         @RequestParam("arquivo") MultipartFile arquivo) {
        return transparenciaService.adicionarDocumento(secaoId, titulo, arquivo);
    }

    @GetMapping("/documentos/{id}/download")
    public ResponseEntity<Resource> baixarDocumento(@PathVariable Long id) {
        Documento documento = transparenciaService.buscarDocumentoPorId(id);
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
    public void deletarDocumento(@PathVariable Long id) {
        transparenciaService.deletarDocumento(id);
    }
}
