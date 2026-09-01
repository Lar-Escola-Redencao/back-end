package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.RedefinirSenhaDTO;
import br.org.larescolaredencao.dto.SolicitarRecuperacaoSenhaDTO;
import br.org.larescolaredencao.dto.ValidarCodigoDTO;
import br.org.larescolaredencao.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Void> solicitarRecuperacao(@Valid @RequestBody SolicitarRecuperacaoSenhaDTO dto) {
        recuperacaoSenhaService.solicitarRecuperacao(dto.getEmail());
        return ResponseEntity.ok().build();
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