package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.AtualizarUsuarioDTO;
import br.org.larescolaredencao.dto.AtualizarVinculoDTO;
import br.org.larescolaredencao.dto.CadastroUsuarioCompletoDTO;
import br.org.larescolaredencao.dto.ContatoDTO;
import br.org.larescolaredencao.dto.InativarUsuarioDTO;
import br.org.larescolaredencao.dto.TransferirTurmaDTO;
import br.org.larescolaredencao.dto.UsuarioResponseDTO;
import br.org.larescolaredencao.dto.VincularContatoExistenteDTO;
import br.org.larescolaredencao.model.ArquivoSaude;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.TipoDocumento;
import br.org.larescolaredencao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponseDTO> listarUsuarios(@AuthenticationPrincipal Membro membroLogado,
                                                   @RequestParam(name = "search", required = false) String search) {
        return usuarioService.listarUsuariosDoMembro(membroLogado.getId(), search);
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarUsuario(@PathVariable("id") Integer id, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.buscarUsuarioPorId(id, membroLogado);
    }

    @GetMapping("/parentescos")
    public Parentesco[] listarParentescos() {
        return Parentesco.values();
    }

    @GetMapping("/tipos-documento")
    public TipoDocumento[] listarTiposDocumento() {
        return TipoDocumento.values();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO criarUsuario(@Valid @RequestBody CadastroUsuarioCompletoDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.cadastrarUsuario(dto, membroLogado);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO criarUsuarioComArquivos(@Valid @RequestPart("dados") CadastroUsuarioCompletoDTO dto,
                                                       @RequestPart(value = "arquivosSaude", required = false) List<MultipartFile> arquivosSaude,
                                                       @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.cadastrarUsuarioComArquivos(dto, arquivosSaude, membroLogado);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO atualizarUsuario(@PathVariable("id") Integer id, @Valid @RequestBody AtualizarUsuarioDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.atualizarUsuario(id, dto, membroLogado);
    }

    @PostMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizarFotoPerfil(@PathVariable("id") Integer id, @RequestParam("foto") MultipartFile foto, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.atualizarFotoPerfil(id, foto, membroLogado);
    }

    @PutMapping("/{id}/turma")
    public UsuarioResponseDTO transferirTurma(@PathVariable("id") Integer id, @Valid @RequestBody TransferirTurmaDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.transferirTurma(id, dto, membroLogado);
    }

    @PutMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativarUsuario(@PathVariable("id") Integer id, @Valid @RequestBody InativarUsuarioDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.inativarUsuario(id, dto, membroLogado);
    }

    @PostMapping("/{id}/contatos")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO vincularNovoContato(@PathVariable("id") Integer id, @Valid @RequestBody ContatoDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.vincularNovoContato(id, dto, membroLogado);
    }

    @PostMapping("/{idUsuario}/contatos/{idContato}/vincular")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO vincularContatoExistente(@PathVariable("idUsuario") Integer idUsuario, @PathVariable("idContato") Integer idContato, @Valid @RequestBody VincularContatoExistenteDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.vincularContatoExistente(idUsuario, idContato, dto, membroLogado);
    }

    @PutMapping("/{idUsuario}/contatos/{idContato}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizarVinculo(@PathVariable("idUsuario") Integer idUsuario, @PathVariable("idContato") Integer idContato, @Valid @RequestBody AtualizarVinculoDTO dto, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.atualizarVinculo(idUsuario, idContato, dto, membroLogado);
    }

    @DeleteMapping("/{idUsuario}/contatos/{idContato}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularContato(@PathVariable("idUsuario") Integer idUsuario, @PathVariable("idContato") Integer idContato, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.desvincularContato(idUsuario, idContato, membroLogado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarUsuario(@PathVariable("id") Integer id, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.deletarUsuario(id, membroLogado);
    }
    
    @GetMapping("/buscar")
    public List<UsuarioResponseDTO> buscarUsuariosAutocomplete(@RequestParam("termo") String termo, @RequestParam("unidadeId") Integer unidadeId, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.buscarUsuariosAutocomplete(termo, unidadeId, membroLogado);
    }

    @GetMapping("/{id}/arquivos-saude")
    public List<ArquivoSaude> listarArquivosSaude(@PathVariable("id") Integer id, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.listarArquivosSaude(id, membroLogado);
    }

    @PostMapping("/{id}/arquivos-saude")
    @ResponseStatus(HttpStatus.CREATED)
    public ArquivoSaude uploadArquivoSaude(@PathVariable("id") Integer id, @RequestParam("titulo") String titulo, @RequestParam("arquivo") MultipartFile arquivo, @AuthenticationPrincipal Membro membroLogado) {
        return usuarioService.uploadArquivoSaude(id, titulo, arquivo, membroLogado);
    }

    @DeleteMapping("/arquivos-saude/{idArquivo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarArquivoSaude(@PathVariable("idArquivo") Integer idArquivo, @AuthenticationPrincipal Membro membroLogado) {
        usuarioService.deletarArquivoSaude(idArquivo, membroLogado);
    }
}
