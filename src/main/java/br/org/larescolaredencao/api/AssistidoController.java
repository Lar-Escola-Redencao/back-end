package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AssistidoResponseDTO;
import br.org.larescolaredencao.dto.AtualizarAssistidoDTO;
import br.org.larescolaredencao.dto.AtualizarVinculoDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.CriarAssistidoDTO;
import br.org.larescolaredencao.dto.InativarAssistidoDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.dto.VincularContatoExistenteDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.TipoDocumento;
import br.org.larescolaredencao.service.AssistidoService;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}")
    public AssistidoResponseDTO buscarAssistido(@PathVariable("id") Integer id) {
        return assistidoService.buscarAssistidoPorId(id);
    }

    @GetMapping("/parentescos")
    public Parentesco[] listarParentescos() {
        return Parentesco.values();
    }

    @GetMapping("/tipos-documento")
    public TipoDocumento[] listarTiposDocumento() {
        return TipoDocumento.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssistidoResponseDTO criarAssistido(@Valid @RequestBody CriarAssistidoDTO dto) {
        return assistidoService.cadastrarAssistido(dto);
    }

    @PutMapping("/{id}")
    public AssistidoResponseDTO atualizarAssistido(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarAssistidoDTO dto) {
        return assistidoService.atualizarAssistido(id, dto);
    }

    @PostMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizarFotoPerfil(@PathVariable("id") Integer id, @RequestParam("foto") MultipartFile foto) {
        assistidoService.atualizarFotoPerfil(id, foto);
    }

    @PutMapping("/{id}/turma")
    public AssistidoResponseDTO transferirTurma(@PathVariable("id") Integer id, @Valid @RequestBody TransferirTurmaDTO dto) {
        return assistidoService.transferirTurma(id, dto);
    }

    @PutMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativarAssistido(@PathVariable("id") Integer id, @Valid @RequestBody InativarAssistidoDTO dto) {
        assistidoService.inativarAssistido(id, dto);
    }

    @PostMapping("/{id}/contatos")
    @ResponseStatus(HttpStatus.CREATED)
    public AssistidoResponseDTO vincularNovoContato(@PathVariable("id") Integer id, @Valid @RequestBody ContatoDTO dto) {
        return assistidoService.vincularNovoContato(id, dto);
    }

    @PostMapping("/{idAssistido}/contatos/{idContato}/vincular")
    @ResponseStatus(HttpStatus.CREATED)
    public AssistidoResponseDTO vincularContatoExistente(@PathVariable("idAssistido") Integer idAssistido, @PathVariable("idContato") Integer idContato, @Valid @RequestBody VincularContatoExistenteDTO dto) {
        return assistidoService.vincularContatoExistente(idAssistido, idContato, dto);
    }

    @PutMapping("/{idAssistido}/contatos/{idContato}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizarVinculo(@PathVariable("idAssistido") Integer idAssistido, @PathVariable("idContato") Integer idContato, @Valid @RequestBody AtualizarVinculoDTO dto) {
        assistidoService.atualizarVinculo(idAssistido, idContato, dto);
    }

    @DeleteMapping("/{idAssistido}/contatos/{idContato}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularContato(@PathVariable("idAssistido") Integer idAssistido, @PathVariable("idContato") Integer idContato) {
        assistidoService.desvincularContato(idAssistido, idContato);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarAssistido(@PathVariable("id") Integer id) {
        assistidoService.deletarAssistido(id);
    }
}