package br.org.larescolaredencao.security;

import br.org.larescolaredencao.api.UsuarioController;
import br.org.larescolaredencao.config.WebConfig;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.MembroRepository;
import br.org.larescolaredencao.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita o SecurityConfig real com chamadas diretas à API: fotos de usuários, URLs antigas
 * de /uploads/usuarios/ e diretórios públicos.
 */
@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfig.class, SecurityFilter.class, CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class, WebConfig.class})
class AutorizacaoArquivosInternosTest {

    @TempDir
    static Path uploads;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("app.cors.allowed-origins", () -> "http://localhost:4200");
        registry.add("app.upload.dir", () -> uploads.toString() + "/");
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private MembroRepository membroRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() throws Exception {
        autenticavel("token-admin", "admin@ler.org", "ADMINISTRADOR");
        autenticavel("token-coordenador", "coordenador@ler.org", "COORDENADOR");
        autenticavel("token-monitor", "monitor@ler.org", "MONITOR");

        Files.createDirectories(uploads.resolve("usuarios"));
        Files.writeString(uploads.resolve("usuarios/foto.jpg"), "foto-interna");
        Files.createDirectories(uploads.resolve("eventos"));
        Files.writeString(uploads.resolve("eventos/banner.jpg"), "imagem-publica");
    }

    private void autenticavel(String token, String email, String papel) {
        Membro membro = new Membro();
        membro.setEmail(email);
        membro.setPapel(new Papel(1, papel, ""));
        when(tokenService.getSubject(token)).thenReturn(email);
        when(membroRepository.findByEmail(email)).thenReturn(Optional.of(membro));
    }

    @Test
    void fotoExigeAutenticacao() throws Exception {
        mvc.perform(get("/usuarios/1/foto"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(RespostaErroSeguranca.MENSAGEM_NAO_AUTENTICADO))
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void tokenInvalidoRecebe401() throws Exception {
        when(tokenService.getSubject("adulterado")).thenThrow(new RuntimeException("inválido"));
        mvc.perform(get("/usuarios/1/foto").header(HttpHeaders.AUTHORIZATION, "Bearer adulterado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void monitorConsultaFotoSemCachePublico() throws Exception {
        when(usuarioService.carregarFotoPerfil(eq(1), any())).thenReturn(new ByteArrayResource("img".getBytes()) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        });

        mvc.perform(get("/usuarios/1/foto").header(HttpHeaders.AUTHORIZATION, "Bearer token-monitor"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(content().bytes("img".getBytes()));
    }

    @Test
    void fotoDeOutraUnidadeRetorna403() throws Exception {
        when(usuarioService.carregarFotoPerfil(eq(2), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado à unidade deste usuário."));

        mvc.perform(get("/usuarios/2/foto").header(HttpHeaders.AUTHORIZATION, "Bearer token-coordenador"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403 FORBIDDEN"));
    }

    @Test
    void monitorNaoAlteraFoto() throws Exception {
        mvc.perform(multipart("/usuarios/1/foto").file("foto", "x".getBytes())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-monitor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RespostaErroSeguranca.MENSAGEM_ACESSO_NEGADO));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void urlAntigaDaFotoBloqueadaParaAnonimo() throws Exception {
        mvc.perform(get("/uploads/usuarios/foto.jpg")).andExpect(status().isUnauthorized());
    }

    @Test
    void urlAntigaDaFotoBloqueadaMesmoAutenticado() throws Exception {
        mvc.perform(get("/uploads/usuarios/foto.jpg").header(HttpHeaders.AUTHORIZATION, "Bearer token-admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentoDeSaudeExigeAutenticacao() throws Exception {
        mvc.perform(get("/uploads/usuarios/saude/exame.pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get("/uploads/saude/exame.pdf")).andExpect(status().isUnauthorized());
    }

    @Test
    void diretorioNaoPublicadoBloqueado() throws Exception {
        mvc.perform(get("/uploads/qualquer/arquivo.txt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/uploads/qualquer/arquivo.txt").header(HttpHeaders.AUTHORIZATION, "Bearer token-admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void navegacaoParaForaDoDiretorioPublicoBloqueada() throws Exception {
        mvc.perform(get("/uploads/eventos/%2e%2e/usuarios/foto.jpg"))
                .andExpect(result -> {
                    int codigo = result.getResponse().getStatus();
                    if (codigo == 200) {
                        throw new AssertionError("Arquivo interno servido por navegação de diretório");
                    }
                });
    }

    @Test
    void imagemInstitucionalContinuaPublica() throws Exception {
        mvc.perform(get("/uploads/eventos/banner.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().string("imagem-publica"));
    }
}
