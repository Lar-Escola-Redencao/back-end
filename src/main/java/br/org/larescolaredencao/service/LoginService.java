package br.org.larescolaredencao.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import br.org.larescolaredencao.dto.LoginRequestDTO;
import br.org.larescolaredencao.dto.LoginResponseDTO;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.security.TokenService;

@Service
public class LoginService {
    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final ProtecaoAutenticacaoService protecao;

    public LoginService(AuthenticationManager manager, TokenService tokenService, ProtecaoAutenticacaoService protecao) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.protecao = protecao;
    }

    public LoginResponseDTO entrar(LoginRequestDTO dados, String ip) {
        var tentativa = protecao.iniciarLogin(dados.identificador(), dados.captchaToken(), ip);
        boolean sucesso = false;
        try {
            var autenticacao = manager.authenticate(new UsernamePasswordAuthenticationToken(dados.identificador(), dados.senha()));
            String token = tokenService.gerarToken((Membro) autenticacao.getPrincipal(), Boolean.TRUE.equals(dados.lembrarMe()));
            sucesso = true;
            return new LoginResponseDTO(token);
        } catch (InternalAuthenticationServiceException erro) {
            throw erro;
        } catch (AuthenticationException erro) {
            boolean captchaObrigatorio = protecao.falhouLogin(tentativa);
            throw new ProtecaoAutenticacaoException(HttpStatus.UNAUTHORIZED,
                    "E-mail, CPF ou senha inválidos.", captchaObrigatorio, 0);
        } finally {
            protecao.concluir(tentativa, sucesso);
        }
    }
}
