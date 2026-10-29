package br.org.larescolaredencao.api;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.service.AcompanhamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcompanhamentoControllerTest {
    @Test void tabelasRecebemMetadadosNoContratoContentEPage() throws Exception {
        var service = mock(AcompanhamentoService.class);
        var pagina = PageRequest.of(1, 5);
        when(service.listarOcorrencias(10, "Ana", pagina, null))
                .thenReturn(new PageImpl<OcorrenciaAcompanhamentoDTO>(List.of(), pagina, 7));
        when(service.listarEntrevistas(10, "Ana", pagina, null))
                .thenReturn(new PageImpl<EntrevistaResponseDTO>(List.of(), pagina, 7));
        var mvc = MockMvcBuilders.standaloneSetup(new AcompanhamentoController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()).build();
        for (String tabela : List.of("ocorrencias", "entrevistas")) {
            mvc.perform(get("/acompanhamento/usuarios/10/" + tabela)
                    .param("search", "Ana").param("page", "1").param("size", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page.number").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(7))
                    .andExpect(jsonPath("$.page.totalPages").value(2));
        }
    }
}
