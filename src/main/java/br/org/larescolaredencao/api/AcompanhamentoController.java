package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.AcompanhamentoService;
import jakarta.validation.Valid;
import org.springframework.data.web.PagedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/acompanhamento/usuarios/{usuario}")
public class AcompanhamentoController {
    private final AcompanhamentoService service;
    public AcompanhamentoController(AcompanhamentoService service) { this.service = service; }

    @GetMapping("/ocorrencias")
    public PagedModel<OcorrenciaAcompanhamentoDTO> ocorrencias(@PathVariable("usuario") Integer usuario,
            @RequestParam(name = "search", required = false) String search, Pageable pageable,
            @AuthenticationPrincipal Membro membro) {
        return new PagedModel<>(service.listarOcorrencias(usuario, search, pageable, membro));
    }

    @GetMapping("/entrevistas")
    public PagedModel<EntrevistaResponseDTO> entrevistas(@PathVariable("usuario") Integer usuario,
            @RequestParam(name = "search", required = false) String search, Pageable pageable,
            @AuthenticationPrincipal Membro membro) {
        return new PagedModel<>(service.listarEntrevistas(usuario, search, pageable, membro));
    }

    @PostMapping("/entrevistas")
    @ResponseStatus(HttpStatus.CREATED)
    public EntrevistaResponseDTO criar(@PathVariable("usuario") Integer usuario,
            @Valid @RequestBody EntrevistaDTO dto, @AuthenticationPrincipal Membro membro) {
        return service.salvarEntrevista(usuario, null, dto, membro);
    }

    @PutMapping("/entrevistas/{id}")
    public EntrevistaResponseDTO editar(@PathVariable("usuario") Integer usuario, @PathVariable("id") Integer id,
            @Valid @RequestBody EntrevistaDTO dto, @AuthenticationPrincipal Membro membro) {
        return service.salvarEntrevista(usuario, id, dto, membro);
    }

    @DeleteMapping("/entrevistas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable("usuario") Integer usuario, @PathVariable("id") Integer id,
            @AuthenticationPrincipal Membro membro) {
        service.excluirEntrevista(usuario, id, membro);
    }
}
