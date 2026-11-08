package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import br.org.larescolaredencao.dto.LoginRequestDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.security.TokenService;

class LoginServiceTest {
    private final AuthenticationManager manager = mock(AuthenticationManager.class);
    private final TokenService tokens = mock(TokenService.class);
    private final ProtecaoAutenticacaoService protecao = new ProtecaoAutenticacaoService(mock(CaptchaPixService.class));
    private final LoginService login = new LoginService(manager, tokens, protecao);

    @Test
    void naoAutenticaSemCaptchaAposCincoFalhas() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Inválido"));
        var dados = new LoginRequestDTO("pessoa@exemplo.com", "errada", false, null);
        for (int i = 0; i < 5; i++) assertThrows(ProtecaoAutenticacaoException.class, () -> login.entrar(dados, "ip"));
        var erro = assertThrows(ProtecaoAutenticacaoException.class, () -> login.entrar(dados, "ip"));
        assertThat(erro.isCaptchaRequired()).isTrue();
        verify(manager, times(5)).authenticate(any());
        verifyNoInteractions(tokens);
    }

    @Test
    void loginInicialValidoPreservaEmissaoDoTokenELembrarMe() {
        Membro membro = mock(Membro.class);
        when(manager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(membro, null));
        when(tokens.gerarToken(membro, true)).thenReturn("jwt");
        var resposta = login.entrar(new LoginRequestDTO("pessoa@exemplo.com", "senha", true, null), "ip");
        assertThat(resposta).isEqualTo(new br.org.larescolaredencao.dto.LoginResponseDTO("jwt"));
    }
}
