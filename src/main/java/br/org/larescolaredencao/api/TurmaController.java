package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarTurmaDTO;
import br.org.larescolaredencao.dto.CriarTurmaDTO;
import br.org.larescolaredencao.dto.TurmaResponseDTO;
import br.org.larescolaredencao.service.TurmaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @GetMapping("/Listar")
    public List<TurmaResponseDTO> listarTurmas(@RequestParam(required = false) Integer unidadeId) {
        return turmaService.listarTurmas(unidadeId);
    }

    @GetMapping("busca/{id}")
    public TurmaResponseDTO buscarTurma(@PathVariable("id") Integer id) {
        return turmaService.buscarTurma(id);
    }

    @PostMapping("/Criar")
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaResponseDTO criarTurma(@Valid @RequestBody CriarTurmaDTO dto) {
        return turmaService.criarTurma(dto);
    }

    @PutMapping("/atualizar/{id}")
    public TurmaResponseDTO atualizarTurma(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarTurmaDTO dto) {
        return turmaService.atualizarTurma(id, dto);
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarTurma(@PathVariable("id") Integer id) {
        turmaService.deletarTurma(id);
    }
}
