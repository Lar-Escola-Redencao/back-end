package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.FrequenciaAcompanhamentoDTO;
import br.org.larescolaredencao.dto.MatriculaHistoricoResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.FrequenciaAcompanhamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/acompanhamento/usuarios/{usuario}/frequencias")
public class FrequenciaAcompanhamentoController {
    private final FrequenciaAcompanhamentoService service;
    public FrequenciaAcompanhamentoController(FrequenciaAcompanhamentoService service) { this.service = service; }

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
