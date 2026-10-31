package br.org.larescolaredencao.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.org.larescolaredencao.service.AgradecimentoPixService;
import br.org.larescolaredencao.service.PixService;

class PixControllerTest {
    private final AgradecimentoPixService agradecimento = mock(AgradecimentoPixService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PixController(mock(PixService.class), agradecimento)).build();

    @Test
    void encaminhaIdentificadorDaRotaEEmailParaAgradecimento() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"doador@example.org\"}"))
                .andExpect(status().isNoContent());
        verify(agradecimento).agradecer("LER123", "doador@example.org");
    }

    @Test
    void rejeitaEmailInvalidoAntesDeEnviarAgradecimento() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"email-invalido\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(agradecimento);
    }
}
