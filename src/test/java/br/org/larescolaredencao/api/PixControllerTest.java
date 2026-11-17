package br.org.larescolaredencao.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.org.larescolaredencao.service.AgradecimentoPixService;
import br.org.larescolaredencao.service.PixService;
import br.org.larescolaredencao.service.BloqueioAgradecimentoException;

class PixControllerTest {
    private final AgradecimentoPixService agradecimento = mock(AgradecimentoPixService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PixController(mock(PixService.class), agradecimento))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void encaminhaIdentificadorDaRotaEEmailParaAgradecimento() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"doador@example.org\",\"captchaToken\":\"token\"}"))
                .andExpect(status().isNoContent());
        verify(agradecimento).agradecer("LER123", "doador@example.org", "token", "127.0.0.1");
    }

    @Test
    void rejeitaEmailInvalidoAntesDeEnviarAgradecimento() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"email-invalido\",\"captchaToken\":\"token\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(agradecimento);
    }

    @Test
    void rejeitaCaptchaAusenteEmChamadaDireta() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"doador@example.org\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(agradecimento);
    }

    @Test
    void retornaTempoRestanteNoCorpoENoHeader() throws Exception {
        doThrow(new BloqueioAgradecimentoException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde.", 42))
                .when(agradecimento).agradecer("LER123", "doador@example.org", "token", "127.0.0.1");
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"doador@example.org\",\"captchaToken\":\"token\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42));
    }

    @Test
    void naoConfiaEmIpInformadoPeloCliente() throws Exception {
        mvc.perform(post("/pix/codigo/LER123/agradecimento")
                .header("X-Forwarded-For", "203.0.113.5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"doador@example.org\",\"captchaToken\":\"token\"}"))
                .andExpect(status().isNoContent());
        verify(agradecimento).agradecer("LER123", "doador@example.org", "token", "127.0.0.1");
    }
}
