package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AtualizarFrequenciaDTO;
import br.org.larescolaredencao.dto.FrequenciaUsuarioResponseDTO;
import br.org.larescolaredencao.dto.SalvarFrequenciaEmLoteDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.FrequenciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/frequencia")
public class FrequenciaController {

    private final FrequenciaService frequenciaService;

    public FrequenciaController(FrequenciaService frequenciaService) {
        this.frequenciaService = frequenciaService;
    }

    @GetMapping
    public List<FrequenciaUsuarioResponseDTO> listarFrequencia(
            @RequestParam("idTurma") Integer idTurma,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Membro membroLogado) {
        return frequenciaService.listarFrequencia(idTurma, data, membroLogado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void salvarFrequenciaEmLote(
            @Valid @RequestBody SalvarFrequenciaEmLoteDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        frequenciaService.salvarFrequenciaEmLote(dto, membroLogado);
    }

    @PutMapping("/{id}")
    public FrequenciaUsuarioResponseDTO atualizarFrequencia(
            @PathVariable("id") Integer id,
            @Valid @RequestBody AtualizarFrequenciaDTO dto,
            @AuthenticationPrincipal Membro membroLogado) {
        return frequenciaService.atualizarFrequencia(id, dto, membroLogado);
    }
}