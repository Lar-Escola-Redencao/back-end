package br.org.larescolaredencao.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {
    @Test
    void deveManterTratamentoEspecificoDeAutenticacao() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler(), new AuthExceptionHandler()).build();
        assertThat(mvc.perform(get("/teste-auth")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
    @Test
    void deveManterStatusDeCorpoInvalido() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var resposta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/teste-corpo").contentType("application/json").content("{invalido"))
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).isEqualTo(400);
    }
    @Test
    void deveManterStatusDeAcessoNegado() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        assertThat(mvc.perform(get("/teste-acesso-negado")).andReturn().getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    void deveManterStatusDeRecursoNaoEncontrado() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        assertThat(mvc.perform(get("/nao-existe")).andReturn().getResponse().getStatus()).isEqualTo(404);
    }
    @Test
    void deveInformarLimitesPorArquivoEPorEnvio() {
        var resposta = new GlobalExceptionHandler().handleArquivoMuitoGrande(new MaxUploadSizeExceededException(-1));
        assertThat(resposta.getStatusCode().value()).isEqualTo(413);
        assertThat(resposta.getBody().get("message")).contains("10 MB", "55 MB");
    }

    @Test
    void deveOcultarDetalhesTecnicosDoErroDeCompilacao() {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        assertThatCode(() -> {
            var resposta = mvc.perform(get("/teste-erro")).andReturn().getResponse();
            assertThat(resposta.getStatus()).isEqualTo(500);
            assertThat(resposta.getContentAsString()).contains("Não foi possível")
                    .doesNotContain("Thumbnails", "Unresolved", "java.lang");
        }).doesNotThrowAnyException();
    }

    enum Formato { PDF, XLSX }

    @Test
    void deveRetornar400ParaParametroInvalido() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ControllerComErro())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var enumInvalido = mvc.perform(get("/teste-params").param("formato", "CSV")).andReturn().getResponse();
        assertThat(enumInvalido.getStatus()).isEqualTo(400);
        assertThat(enumInvalido.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("formato", "PDF", "XLSX");
        var dataInvalida = mvc.perform(get("/teste-params").param("inicio", "2026-02-30")).andReturn().getResponse();
        assertThat(dataInvalida.getStatus()).isEqualTo(400);
        assertThat(dataInvalida.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("inicio");
    }

    @RestController
    static class ControllerComErro {
        @GetMapping("/teste-params")
        String params(@org.springframework.web.bind.annotation.RequestParam(name = "formato", required = false) Formato formato,
                @org.springframework.web.bind.annotation.RequestParam(name = "inicio", required = false)
                @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                java.time.LocalDate inicio) {
            return "ok";
        }

        @GetMapping("/teste-auth")
        String auth() {
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas");
        }
        @org.springframework.web.bind.annotation.PostMapping("/teste-corpo")
        String corpo(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> dados) {
            return "ok";
        }
        @GetMapping("/teste-acesso-negado")
        String proibido() {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado");
        }
        @GetMapping("/teste-erro")
        String erro() {
            throw new Error("Unresolved compilation problems: Thumbnails cannot be resolved");
        }
    }
}
