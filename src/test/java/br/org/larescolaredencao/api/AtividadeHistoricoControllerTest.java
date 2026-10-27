package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.ResponsavelHistoricoDTO;
import br.org.larescolaredencao.service.AtividadeHistoricoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AtividadeHistoricoControllerTest {
    @Test
    void buscaResponsaveisSemDependerDeNomesDeParametrosDoCompilador() throws Exception {
        var service = mock(AtividadeHistoricoService.class);
        var pagina = PageRequest.of(0, 5);
        when(service.buscarResponsaveis("Ana", 3, pagina)).thenReturn(
                new PageImpl<ResponsavelHistoricoDTO>(List.of(new ResponsavelHistoricoDTO(3, "Ana")), pagina, 1L));
        var mvc = MockMvcBuilders.standaloneSetup(new AtividadeHistoricoController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
        mvc.perform(get("/historico-alteracoes/atividades/responsaveis/sugestoes")
                        .param("search", "Ana").param("id", "3").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].nome").value("Ana"));
        verify(service).buscarResponsaveis("Ana", 3, pagina);
    }

    @Test
    void carregaPrimeiraPaginaSemBuscaOuResponsavelSelecionado() throws Exception {
        var service = mock(AtividadeHistoricoService.class);
        when(service.buscarResponsaveis(null, null, PageRequest.of(0, 5))).thenReturn(new PageImpl<>(List.of()));
        var mvc = MockMvcBuilders.standaloneSetup(new AtividadeHistoricoController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
        mvc.perform(get("/historico-alteracoes/atividades/responsaveis/sugestoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        verify(service).buscarResponsaveis(null, null, PageRequest.of(0, 5));
    }
}
