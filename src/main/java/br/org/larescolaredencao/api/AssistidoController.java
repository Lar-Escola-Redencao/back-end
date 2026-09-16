package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AssistidoResponseDTO;
import br.org.larescolaredencao.dto.CriarAssistidoDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.AssistidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/assistidos")
public class AssistidoController {

    private final AssistidoService assistidoService;

    public AssistidoController(AssistidoService assistidoService) {
        this.assistidoService = assistidoService;
    }

    @GetMapping
    public List<AssistidoResponseDTO> listarAssistidos(@AuthenticationPrincipal Membro membroLogado) {
        return assistidoService.listarAssistidosDoMembro(membroLogado.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssistidoResponseDTO criarAssistido(@Valid @RequestBody CriarAssistidoDTO dto) {
        return assistidoService.cadastrarAssistido(dto);
    }
}