package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.TokenRecuperacaoRepository;

class RecuperacaoSenhaCaptchaTest {
    private final MembroRepository membros = mock(MembroRepository.class);
    private final TokenRecuperacaoRepository codigos = mock(TokenRecuperacaoRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final CaptchaPixService captcha = mock(CaptchaPixService.class);
    private final RecuperacaoSenhaService recuperacao = new RecuperacaoSenhaService(membros, codigos, email,
            mock(PasswordEncoder.class), new ProtecaoAutenticacaoService(captcha));

    @Test
    void verificaCaptchaAntesDeConsultarContaOuEnviarEmail() {
        assertThrows(ProtecaoAutenticacaoException.class,
                () -> recuperacao.solicitarRecuperacao("pessoa@exemplo.com", null, "ip"));
        verifyNoInteractions(membros, codigos, email, captcha);
    }

    @Test
    void contaInexistenteTambemTemCooldownSemExporCadastro() {
        when(membros.findByEmail("pessoa@exemplo.com")).thenReturn(Optional.empty());
        recuperacao.solicitarRecuperacao("PESSOA@exemplo.com", "token", "ip");
        var erro = assertThrows(ProtecaoAutenticacaoException.class,
                () -> recuperacao.solicitarRecuperacao("pessoa@exemplo.com", "outro-token", "outro-ip"));
        assertThat(erro.getRetryAfterSeconds()).isEqualTo(60);
        verify(membros, times(1)).findByEmail("pessoa@exemplo.com");
        verifyNoInteractions(codigos, email);
    }
}
