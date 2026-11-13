package br.org.larescolaredencao.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.repository.PapelRepository;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {
    @Mock MembroRepository membros;
    @InjectMocks SecurityFilter filtro;
    TokenService tokens = new TokenService();
    Membro membro;
    String token;

    @BeforeEach
    void preparar() throws Exception {
        ReflectionTestUtils.setField(tokens, "secret", "segredo-de-teste");
        PapelRepository papeis = mock(PapelRepository.class);
        when(papeis.findByMembroId(1)).thenReturn(Optional.of(new Papel(3, "MONITOR", null)));
        ReflectionTestUtils.setField(tokens, "papelRepository", papeis);
        ReflectionTestUtils.setField(filtro, "tokenService", tokens);
        membro = new Membro(1, "Fulano", "fulano@example.com", "senha", "12345678900", null, null,
                new Papel(3, "MONITOR", null));
        token = tokens.gerarToken(membro, false);
        when(membros.findByEmail("fulano@example.com")).thenReturn(Optional.of(membro));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private boolean autenticou() throws Exception {
        var requisicao = new MockHttpServletRequest();
        requisicao.addHeader("Authorization", "Bearer " + token);
        filtro.doFilter(requisicao, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void autenticaMembroAtivoComSessaoVigente() throws Exception {
        assertThat(autenticou()).isTrue();
    }

    @Test
    void sessaoAbertaDeMembroDesativadoDeixaDeValer() throws Exception {
        membro.setAtivo(false);
        membro.invalidarSessoes();
        assertThat(autenticou()).isFalse();
    }

    @Test
    void sessaoAnteriorContinuaInvalidaDepoisDaReativacao() throws Exception {
        membro.setAtivo(false);
        membro.invalidarSessoes();
        membro.setAtivo(true);
        membro.invalidarSessoes();
        assertThat(autenticou()).isFalse();
    }
}
