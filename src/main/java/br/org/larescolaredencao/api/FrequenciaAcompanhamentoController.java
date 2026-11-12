package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.FrequenciaAcompanhamentoDTO;
import br.org.larescolaredencao.dto.MatriculaHistoricoResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import br.org.larescolaredencao.service.FrequenciaAcompanhamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.org.larescolaredencao.service.relatorio.ArquivoRelatorio;
import br.org.larescolaredencao.service.relatorio.RelatorioAcompanhamentoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/acompanhamento/usuarios/{usuario}/frequencias")
public class FrequenciaAcompanhamentoController {
    private final FrequenciaAcompanhamentoService service;
    private final RelatorioAcompanhamentoService relatorio;
    public FrequenciaAcompanhamentoController(FrequenciaAcompanhamentoService service,
            RelatorioAcompanhamentoService relatorio) {
        this.service = service; this.relatorio = relatorio;
    }

    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> relatorio(@PathVariable("usuario") Integer usuario,
            @RequestParam(name = "matricula", required = false) Integer matricula,
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(name = "formato", defaultValue = "PDF") FormatoRelatorio formato,
            @AuthenticationPrincipal Membro membro) {
        ArquivoRelatorio arquivo = relatorio.gerar(usuario, matricula, inicio, fim, membro, formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(arquivo.tipoConteudo()))
                .contentLength(arquivo.conteudo().length)
                .body(arquivo.conteudo());
    }

    @GetMapping
    public FrequenciaAcompanhamentoDTO consultar(@PathVariable("usuario") Integer usuario,
            @RequestParam(name = "matricula", required = false) Integer matricula,
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Membro membro) {
        return service.consultar(usuario, matricula, inicio, fim, membro);
    }

    @GetMapping("/matriculas")
    public List<MatriculaHistoricoResponseDTO> matriculas(@PathVariable("usuario") Integer usuario,
            @AuthenticationPrincipal Membro membro) {
        return service.matriculas(usuario, membro);
    }
}
