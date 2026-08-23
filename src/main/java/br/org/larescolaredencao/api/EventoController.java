package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
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
    public List<EventoResponseDTO> listarEventos() {
        return eventoService.getAllEventos();
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscarEvento(@PathVariable("id") Integer id) {
        return eventoService.getEventoById(id);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponseDTO criarEvento(@Valid @ModelAttribute CriarEventoDTO criarEventoDTO) {
        return eventoService.criarEvento(criarEventoDTO);
    }

    @PutMapping("/atualizar/{id}")
    public EventoResponseDTO atualizarEvento(@PathVariable("id") Integer id, @Valid @ModelAttribute AtualizarEventoDTO atualizarEventoDTO) {
        return eventoService.atualizarEvento(id, atualizarEventoDTO);
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarEvento(@PathVariable("id") Integer id) {
        eventoService.deletarEvento(id);
    }
}