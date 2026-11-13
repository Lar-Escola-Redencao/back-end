package br.org.larescolaredencao.api;

import br.org.larescolaredencao.service.MembroService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembroOrdenacaoControllerTest {

    private final MembroService service = mock(MembroService.class);

    private MockMvc criarMvc() {
        when(service.getAllMembros(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenAnswer(invocacao -> Page.empty(invocacao.getArgument(0)));
        return MockMvcBuilders.standaloneSetup(new MembroController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void consultaInicialIncluiTodosComAtivosPrimeiroECadastrosRecentesAntesDaPaginacao() throws Exception {
        criarMvc().perform(get("/membro/todos").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        var pagina = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "ativo", "id"));
        verify(service).getAllMembros(pagina, null, null, null, null, null);
    }

    @Test
    void permiteEscolherOutraOrdenacaoNaConsulta() throws Exception {
        criarMvc().perform(get("/membro/todos").param("sort", "nomeCompleto,asc"))
                .andExpect(status().isOk());

        var pagina = PageRequest.of(0, 10, Sort.by("nomeCompleto"));
        verify(service).getAllMembros(pagina, null, null, null, null, null);
    }
}
