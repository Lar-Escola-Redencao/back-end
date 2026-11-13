package br.org.larescolaredencao.api;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.security.*;
import br.org.larescolaredencao.service.ArquivoService;
import br.org.larescolaredencao.service.PaginaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(TransparenciaOrdemSegurancaTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class TransparenciaOrdemSegurancaTest {
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class, SecurityFilter.class, CustomAuthenticationEntryPoint.class,
            CustomAccessDeniedHandler.class, PaginaController.class})
    static class Config {
    }
    @Autowired WebApplicationContext context;
    @MockitoBean TokenService tokens;
    @MockitoBean MembroRepository membros;
    @MockitoBean PaginaService paginas;
    @MockitoBean ArquivoService arquivos;
    MockMvc mvc;
    @BeforeEach void preparar() {
        reset(tokens, membros, paginas);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(FilterChainProxy.class)).build();
    }
    private void autenticar(String papel) {
        var membro = mock(Membro.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + papel))).when(membro).getAuthorities();
        when(tokens.getSubject("token-teste")).thenReturn("admin@teste.local");
        when(membros.findByEmail("admin@teste.local")).thenReturn(Optional.of(membro));
    }
    @Test void anonimatoNaoPodeConsultarNemSalvarOrdem() throws Exception {
        mvc.perform(get("/paginas/1/secoes/ordenacao")).andExpect(status().isUnauthorized());
        mvc.perform(put("/paginas/secoes/reordenar").contentType("application/json").content("[]"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(paginas);
    }
    @Test void coordenadorNaoPodeConsultarNemSalvarOrdem() throws Exception {
        autenticar("COORDENADOR");
        mvc.perform(get("/paginas/1/secoes/ordenacao").header("Authorization", "Bearer token-teste"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/paginas/secoes/reordenar").header("Authorization", "Bearer token-teste")
                .contentType("application/json").content("[]")).andExpect(status().isForbidden());
        verifyNoInteractions(paginas);
    }
    @Test void administradorPodeConsultarESalvarOrdem() throws Exception {
        autenticar("ADMINISTRADOR");
        when(paginas.listarSecoesParaOrdenacao()).thenReturn(List.of());
        mvc.perform(get("/paginas/1/secoes/ordenacao").header("Authorization", "Bearer token-teste"))
                .andExpect(status().isOk());
        mvc.perform(put("/paginas/secoes/reordenar").header("Authorization", "Bearer token-teste")
                .contentType("application/json").content("[{\"id\":10,\"ordem\":1}]"))
                .andExpect(status().isNoContent());
        verify(paginas).reordenarSecoes(anyList());
    }
}
