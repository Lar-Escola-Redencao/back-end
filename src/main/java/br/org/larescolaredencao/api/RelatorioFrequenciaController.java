package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.RelatorioPresencasDTO;
import br.org.larescolaredencao.dto.RelatorioResumoDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.relatorio.ArquivoRelatorio;
import br.org.larescolaredencao.service.relatorio.RelatorioFrequenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/frequencia/relatorios")
public class RelatorioFrequenciaController {

    private final RelatorioFrequenciaService relatorioService;

    public RelatorioFrequenciaController(RelatorioFrequenciaService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @PostMapping("/presencas")
    public ResponseEntity<byte[]> exportarPresencas(
            @Valid @RequestBody RelatorioPresencasDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        return arquivo(relatorioService.gerarPresencas(dto, membroLogado));
    }

    @PostMapping("/resumo")
    public ResponseEntity<byte[]> exportarResumo(
            @Valid @RequestBody RelatorioResumoDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        return arquivo(relatorioService.gerarResumo(dto, membroLogado));
    }

    private ResponseEntity<byte[]> arquivo(ArquivoRelatorio arquivo) {
        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(arquivo.tipoConteudo()))
                .contentLength(arquivo.conteudo().length)
                .body(arquivo.conteudo());
    }
}
