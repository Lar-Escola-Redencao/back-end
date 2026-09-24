package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AtualizarContatoDTO;
import br.org.larescolaredencao.dto.ContatoListagemDTO;
import br.org.larescolaredencao.dto.VinculoContatoResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.ContatoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/contatos")
public class ContatoController {

    private final ContatoService contatoService;

    public ContatoController(ContatoService contatoService) {
        this.contatoService = contatoService;
    }

    @GetMapping
    public PagedModel<ContatoListagemDTO> listarContatos(@AuthenticationPrincipal Membro membroLogado, Pageable pageable) {
        return new PagedModel<>(contatoService.listarContatos(membroLogado.getId(), pageable));
    }

    @GetMapping("/{id}")
    public ContatoListagemDTO buscarContato(@PathVariable("id") Integer id) {
        return contatoService.buscarContato(id);
    }

    @GetMapping("/buscar")
    public List<ContatoListagemDTO> buscarContatosAutocomplete(@AuthenticationPrincipal Membro membroLogado, @RequestParam("termo") String termo) {
        return contatoService.buscarContatosAutocomplete(membroLogado.getId(), termo);
    }

    @GetMapping("/{id}/vinculos")
    public List<VinculoContatoResponseDTO> listarVinculosDoContato(@PathVariable("id") Integer id) {
        return contatoService.listarVinculosDoContato(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContatoListagemDTO criarContato(@Valid @RequestBody AtualizarContatoDTO dto) {
        return contatoService.criarContatoAvulso(dto);
    }

    @PutMapping("/{id}")
    public ContatoListagemDTO atualizarContato(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarContatoDTO dto) {
        return contatoService.atualizarContato(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarContato(@PathVariable("id") Integer id) {
        contatoService.deletarContato(id);
    }
}