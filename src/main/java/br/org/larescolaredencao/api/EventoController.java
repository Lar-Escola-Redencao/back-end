package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoDetalhadoResponseDTO;
import br.org.larescolaredencao.dto.EventoRedeSocialResponseDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.dto.VincularRedeSocialEventoDTO;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.service.EventoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/evento")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/todos")
    public PagedModel<EventoResponseDTO> listarEventos(Pageable pageable,
            @RequestParam(name = "tipo", required = false) TipoEvento tipo) {
        return new PagedModel<>(eventoService.getAllEventos(pageable, tipo));
    }

    @GetMapping("/{id}")
    public EventoDetalhadoResponseDTO buscarEvento(@PathVariable("id") Integer id) {
        return eventoService.getEventoById(id);
    }
    
    @GetMapping("/tipos")
    public TipoEvento[] listarTipos() {
        return TipoEvento.values();
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponseDTO criarEvento(@Valid @ModelAttribute CriarEventoDTO criarEventoDTO) {
        return eventoService.criarEvento(criarEventoDTO);
    }

    @PutMapping("/{id}")
    public EventoResponseDTO atualizarEvento(@PathVariable("id") Integer id, @Valid @ModelAttribute AtualizarEventoDTO atualizarEventoDTO) {
        return eventoService.atualizarEvento(id, atualizarEventoDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarEvento(@PathVariable("id") Integer id) {
        eventoService.deletarEvento(id);
    }

    @GetMapping("/{id}/redes-sociais")
    public List<EventoRedeSocialResponseDTO> listarRedesSociais(@PathVariable("id") Integer id) {
        return eventoService.listarRedesSociais(id);
    }

    @PostMapping("/{id}/redes-sociais")
    public EventoRedeSocialResponseDTO vincularRedeSocial(@PathVariable("id") Integer id,
            @Valid @RequestBody VincularRedeSocialEventoDTO dto) {
        return eventoService.vincularRedeSocial(id, dto);
    }

    @DeleteMapping("/{id}/redes-sociais/{idRedeSocial}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularRedeSocial(@PathVariable("id") Integer id,
            @PathVariable("idRedeSocial") Long idRedeSocial) {
        eventoService.desvincularRedeSocial(id, idRedeSocial);
    }
}
