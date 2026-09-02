package br.org.larescolaredencao.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
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

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
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
            @RequestParam(required = false) TipoEvento tipo) {
        return new PagedModel<>(eventoService.getAllEventos(pageable, tipo));
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscarEvento(@PathVariable("id") Integer id) {
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
}