package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AtividadeHistoricoDTO;
import br.org.larescolaredencao.dto.ConsultaAtividadesDTO;
import br.org.larescolaredencao.dto.LeituraHistoricoDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.dto.EstadoLeituraHistoricoDTO;
import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;
import br.org.larescolaredencao.service.AtividadeHistoricoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/historico-alteracoes/atividades")
public class AtividadeHistoricoController {

    private final AtividadeHistoricoService atividadeHistoricoService;

    public AtividadeHistoricoController(AtividadeHistoricoService atividadeHistoricoService) {
        this.atividadeHistoricoService = atividadeHistoricoService;
    }

    @GetMapping
    public PagedModel<AtividadeHistoricoDTO> listar(
            @Valid @ModelAttribute ConsultaAtividadesDTO filtro,
            @PageableDefault(size = 10) Pageable pageable) {
        return new PagedModel<>(atividadeHistoricoService.listar(filtro, pageable));
    }

    @GetMapping("/responsaveis")
    public List<ResponsavelHistoricoDTO> listarResponsaveis() {
        return atividadeHistoricoService.listarResponsaveis();
    }

    @GetMapping("/responsaveis/sugestoes")
    public PagedModel<ResponsavelHistoricoDTO> buscarResponsaveis(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer id,
            @PageableDefault(size = 5) Pageable pageable) {
        return new PagedModel<>(atividadeHistoricoService.buscarResponsaveis(search, id, pageable));
    }

    @GetMapping("/leitura")
    public EstadoLeituraHistoricoDTO consultarLeitura(@Valid @ModelAttribute ConsultaAtividadesDTO filtro,
                                         @AuthenticationPrincipal Membro administrador) {
        return atividadeHistoricoService.consultarLeitura(filtro, administrador.getId());
    }

    @PutMapping("/leitura")
    public void registrarLeitura(@Valid @ModelAttribute ConsultaAtividadesDTO filtro,
                                @Valid @RequestBody LeituraHistoricoDTO leitura,
                                @AuthenticationPrincipal Membro administrador) {
        atividadeHistoricoService.registrarLeitura(filtro, administrador.getId(), leitura.ultimoEvento());
    }
}
