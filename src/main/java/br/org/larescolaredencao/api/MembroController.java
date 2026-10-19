package br.org.larescolaredencao.api;

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

import br.org.larescolaredencao.dto.AtualizarMembroDTO;
import br.org.larescolaredencao.dto.AtualizarPerfilDTO;
import br.org.larescolaredencao.dto.AlterarSenhaDTO;
import br.org.larescolaredencao.dto.ValidarSenhaAtualDTO;
import br.org.larescolaredencao.dto.CriarMembroDTO;
import br.org.larescolaredencao.dto.MembroResponseDTO;
import br.org.larescolaredencao.model.Membro;
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
    public PagedModel<MembroResponseDTO> listarMembros(Pageable pageable,
            @RequestParam(name = "idPapel", required = false) Integer idPapel,
            @RequestParam(name = "idUnidade", required = false) Integer idUnidade,
            @RequestParam(name = "search", required = false) String search,
            @AuthenticationPrincipal Membro membroLogado) {
        return new PagedModel<>(membroService.getAllMembros(pageable, idPapel, search, idUnidade, membroLogado));
    }

    @GetMapping("/{id}")
    public MembroResponseDTO buscarMembro(@PathVariable("id") Integer id,
            @AuthenticationPrincipal Membro membroLogado) {
        return membroService.getMembroById(id, membroLogado);
    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public MembroResponseDTO criarMembro(@Valid @RequestBody CriarMembroDTO criarMembroDTO,
            @AuthenticationPrincipal Membro membroLogado) {
        return membroService.criarMembro(criarMembroDTO, membroLogado);
    }

    @PutMapping("/{id}")
    public MembroResponseDTO atualizarMembro(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarMembroDTO atualizarMembroDTO,
                                             @AuthenticationPrincipal Membro membroLogado) {
        return membroService.atualizarMembro(id, atualizarMembroDTO, membroLogado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarMembro(@PathVariable("id") Integer id, @AuthenticationPrincipal Membro membroLogado) {
        membroService.deletarMembro(id, membroLogado);
    }
    
    @GetMapping("/me")
    public MembroResponseDTO meuPerfil(@AuthenticationPrincipal Membro membroLogado) {
        return membroService.buscarPerfil(membroLogado.getId());
    }

    @PutMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarMinhaSenha(@AuthenticationPrincipal Membro membroLogado,
                                 @Valid @RequestBody AlterarSenhaDTO dto) {
        membroService.alterarSenha(membroLogado.getId(), dto);
    }

    @PostMapping("/me/senha/validar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validarMinhaSenha(@AuthenticationPrincipal Membro membroLogado,
                                 @Valid @RequestBody ValidarSenhaAtualDTO dto) {
        membroService.validarSenhaAtual(membroLogado.getId(), dto.getSenhaAtual());
    }
 
    @PutMapping("/me")
    public MembroResponseDTO atualizarMeuPerfil(@AuthenticationPrincipal Membro membroLogado,
            @Valid @RequestBody AtualizarPerfilDTO dto) {
        return membroService.atualizarPerfil(membroLogado.getId(), dto);
    }
    
}
