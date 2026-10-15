package br.org.larescolaredencao.api;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.service.UsuarioService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class ArquivoSaudeController {

    private final UsuarioService usuarioService;

    public ArquivoSaudeController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/uploads/usuarios/saude/{nome}")
    public ResponseEntity<Resource> carregarArquivo(@PathVariable("nome") String nome,
                                                    @AuthenticationPrincipal Membro membroLogado) {
        return resposta("/uploads/usuarios/saude/" + nome, membroLogado);
    }

    @GetMapping("/uploads/saude/{nome}")
    public ResponseEntity<Resource> carregarArquivoLegado(@PathVariable("nome") String nome,
                                                          @AuthenticationPrincipal Membro membroLogado) {
        return resposta("/uploads/saude/" + nome, membroLogado);
    }

    private ResponseEntity<Resource> resposta(String caminho, Membro membroLogado) {
        Resource recurso = usuarioService.carregarArquivoSaude(caminho, membroLogado);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaTypeFactory.getMediaType(recurso).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(recurso.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(recurso);
    }
}
