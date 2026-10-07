package br.org.larescolaredencao.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.IndicadoresSobreDTO;
import br.org.larescolaredencao.service.SobrePublicoService;

@RestController
@RequestMapping("/paginas/sobre")
public class SobrePublicoController {
    private final SobrePublicoService sobrePublicoService;

    public SobrePublicoController(SobrePublicoService sobrePublicoService) {
        this.sobrePublicoService = sobrePublicoService;
    }

    @GetMapping("/indicadores")
    public IndicadoresSobreDTO obterIndicadores() {
        return sobrePublicoService.obterIndicadores();
    }
}
