package br.org.larescolaredencao.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.service.MembroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/membro")
public class MembroController {

    private final MembroService membroService;

    public MembroController(MembroService membroService) {
        this.membroService = membroService;
    }

    @GetMapping("/todos")
    public PagedModel<MembroResponseDTO> listarMembros(Pageable pageable) {
        return new PagedModel<>(membroService.getAllMembros(pageable));
    }

    @GetMapping("/{id}")
    public MembroResponseDTO buscarMembro(@PathVariable("id") Integer id) {
        return membroService.getMembroById(id);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public MembroResponseDTO criarMembro(@Valid @RequestBody CriarMembroDTO criarMembroDTO) {
        return membroService.criarMembro(criarMembroDTO);
    }

    @PutMapping("/{id}")
    public MembroResponseDTO atualizarMembro(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarMembroDTO atualizarMembroDTO) {
        return membroService.atualizarMembro(id, atualizarMembroDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarMembro(@PathVariable("id") Integer id) {
        membroService.deletarMembro(id);
    }
}