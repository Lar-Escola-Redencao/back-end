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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarUnidadeDTO;
import br.org.larescolaredencao.dto.CriarUnidadeDTO;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.service.UnidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/unidade")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping("/todas")
    public List<Unidade> listarUnidades() {
        return unidadeService.getAllUnidades();
    }

    @GetMapping("/{id}")
    public Unidade buscarUnidade(@PathVariable("id") Integer id) {
        return unidadeService.getUnidadeById(id);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public Unidade criarUnidade(@Valid @RequestBody CriarUnidadeDTO criarUnidadeDTO) {
        return unidadeService.criarUnidade(criarUnidadeDTO);
    }

    @PutMapping("/{id}")
    public Unidade atualizarUnidade(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarUnidadeDTO atualizarUnidadeDTO) {
        return unidadeService.atualizarUnidade(id, atualizarUnidadeDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarUnidade(@PathVariable("id") Integer id) {
        unidadeService.deletarUnidade(id);
    }
}
