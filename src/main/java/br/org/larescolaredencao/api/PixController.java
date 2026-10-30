package br.org.larescolaredencao.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.CodigoPixResponseDTO;
import br.org.larescolaredencao.dto.ConfiguracaoPixResponseDTO;
import br.org.larescolaredencao.dto.GerarCodigoPixDTO;
import br.org.larescolaredencao.service.PixService;
import br.org.larescolaredencao.service.AgradecimentoPixService;
import br.org.larescolaredencao.dto.AgradecimentoPixDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/pix")
public class PixController {

    private final PixService pixService;
    private final AgradecimentoPixService agradecimentoPixService;

    public PixController(PixService pixService, AgradecimentoPixService agradecimentoPixService) {
        this.pixService = pixService;
        this.agradecimentoPixService = agradecimentoPixService;
    }

    @GetMapping
    public ResponseEntity<ConfiguracaoPixResponseDTO> obterConfiguracao() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pixService.obterConfiguracao());
    }

    @PostMapping("/codigo")
    public ResponseEntity<CodigoPixResponseDTO> gerarCodigo(@Valid @RequestBody GerarCodigoPixDTO dto) {
        CodigoPixResponseDTO codigo = pixService.gerarCodigo(dto);
        agradecimentoPixService.registrar(codigo);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(codigo);
    }

    @PostMapping("/codigo/{identificador}/agradecimento")
    public ResponseEntity<Void> agradecer(@PathVariable String identificador,
            @Valid @RequestBody AgradecimentoPixDTO dto) {
        agradecimentoPixService.agradecer(identificador, dto.email());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
