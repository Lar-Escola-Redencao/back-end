package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.RedefinirSenhaDTO;
import br.org.larescolaredencao.dto.RecuperacaoSenhaRespostaDTO;
import br.org.larescolaredencao.dto.SolicitarRecuperacaoSenhaDTO;
import br.org.larescolaredencao.dto.ValidarCodigoDTO;
import br.org.larescolaredencao.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public RecuperacaoSenhaController(RecuperacaoSenhaService recuperacaoSenhaService) {
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/esqueci-minha-senha")
    public ResponseEntity<RecuperacaoSenhaRespostaDTO> solicitarRecuperacao(@Valid @RequestBody SolicitarRecuperacaoSenhaDTO dto, HttpServletRequest request) {
        return ResponseEntity.ok(recuperacaoSenhaService.solicitarRecuperacao(
                dto.getEmail(), dto.getCaptchaToken(), request.getRemoteAddr()));
    }

    @PostMapping("/validar-codigo")
    public ResponseEntity<Void> validarCodigo(@Valid @RequestBody ValidarCodigoDTO dto) {
        recuperacaoSenhaService.validarCodigo(dto.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaDTO dto) {
        recuperacaoSenhaService.redefinirSenha(dto.getToken(), dto.getNovaSenha());
        return ResponseEntity.ok().build();
    }
}
