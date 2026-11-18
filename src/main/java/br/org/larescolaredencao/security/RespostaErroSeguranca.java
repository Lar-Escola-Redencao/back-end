package br.org.larescolaredencao.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Formato único das respostas de ausência de autenticação (401) e acesso negado (403),
 * igual ao produzido pelo GlobalExceptionHandler: {@code {"message": "...", "status": "403 FORBIDDEN"}}.
 */
public final class RespostaErroSeguranca {

    public static final String MENSAGEM_NAO_AUTENTICADO =
            "Sessão ausente, inválida ou expirada. Faça login novamente.";
    public static final String MENSAGEM_ACESSO_NEGADO =
            "Você não tem permissão para acessar este recurso.";

    private RespostaErroSeguranca() {
    }

    public static void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write("{\"message\":\"%s\",\"status\":\"%s\"}".formatted(mensagem, status));
    }
}
