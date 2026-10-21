package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.FiltroHistoricoDTO;
import br.org.larescolaredencao.dto.HistoricoDetalheDTO;
import br.org.larescolaredencao.dto.HistoricoEventoDTO;
import br.org.larescolaredencao.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/historico-alteracoes")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public PagedModel<HistoricoEventoDTO> listar(
            @Valid @ModelAttribute FiltroHistoricoDTO filtro,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(historicoService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    public HistoricoDetalheDTO detalhar(@PathVariable("id") long id) {
        return historicoService.detalhar(id);
    }
}
