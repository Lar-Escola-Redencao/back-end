package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AtualizarOcorrenciaDTO;
import br.org.larescolaredencao.dto.CriarOcorrenciaDTO;
import br.org.larescolaredencao.dto.OcorrenciaResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.OcorrenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocorrencia")
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OcorrenciaResponseDTO criarOcorrencia(
            @Valid @RequestBody CriarOcorrenciaDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        return ocorrenciaService.criarOcorrencia(dto, membroLogado);
    }

    @PutMapping("/{id}")
    public OcorrenciaResponseDTO atualizarOcorrencia(
            @PathVariable("id") Integer id,
            @Valid @RequestBody AtualizarOcorrenciaDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        return ocorrenciaService.atualizarOcorrencia(id, dto, membroLogado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarOcorrencia(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal Membro membroLogado) {
        ocorrenciaService.deletarOcorrencia(id, membroLogado);
    }
}