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

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.service.MembroService;

@RestController
@RequestMapping("/membro")
public class MembroController {

    private final MembroService membroService;

    public MembroController(MembroService membroService) {
        this.membroService = membroService;
    }

    @GetMapping("/todos")
    public List<MembroResponseDTO> listarMembros() {
        return membroService.getAllMembros();
    }

    @GetMapping("/{id}")
    public MembroResponseDTO buscarMembro(@PathVariable Integer id) {
        return membroService.getMembroById(id);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public MembroResponseDTO criarMembro(@RequestBody CriarMembroDTO criarMembroDTO) {
        return membroService.criarMembro(criarMembroDTO);
    }

    @PutMapping("/atualizar/{id}")
    public MembroResponseDTO atualizarMembro(@PathVariable Integer id, @RequestBody AtualizarMembroDTO atualizarMembroDTO) {
        return membroService.atualizarMembro(id, atualizarMembroDTO);
    }

    @DeleteMapping("/deletar/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarMembro(@PathVariable Integer id) {
        membroService.deletarMembro(id);
    }
}