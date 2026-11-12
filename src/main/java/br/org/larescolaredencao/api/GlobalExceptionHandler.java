package br.org.larescolaredencao.api;

import br.org.larescolaredencao.security.RespostaErroSeguranca;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.unit.DataSize;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private DataSize tamanhoMaximoArquivo = DataSize.ofMegabytes(10);

    @Value("${spring.servlet.multipart.max-request-size:55MB}")
    private DataSize tamanhoMaximoEnvio = DataSize.ofMegabytes(55);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        StringBuilder mensagensAgrupadas = new StringBuilder();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            response.put(error.getField(), error.getDefaultMessage());
            if (mensagensAgrupadas.length() > 0) mensagensAgrupadas.append(" | ");
            mensagensAgrupadas.append(error.getDefaultMessage());
        });

        exception.getBindingResult().getGlobalErrors().forEach(error -> {
            if (mensagensAgrupadas.length() > 0) mensagensAgrupadas.append(" | ");
            mensagensAgrupadas.append(error.getDefaultMessage());
        });

        response.put("message", mensagensAgrupadas.length() > 0 ? mensagensAgrupadas.toString() : "Erro de validação");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, String>> handleOrdenacaoInvalida(PropertyReferenceException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("sort", "Campo de ordenação inválido: " + exception.getPropertyName());
        erro.put("message", "Campo de ordenação inválido: " + exception.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleArquivoMuitoGrande(MaxUploadSizeExceededException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("message", "Cada arquivo pode ter até " + tamanhoMaximoArquivo.toMegabytes()
                + " MB. O tamanho total do envio não pode ultrapassar "
                + tamanhoMaximoEnvio.toMegabytes() + " MB.");
        erro.put("status", HttpStatus.CONTENT_TOO_LARGE.toString());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(erro);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("message", exception.getReason());
        erro.put("status", exception.getStatusCode().toString());
        return ResponseEntity.status(exception.getStatusCode()).body(erro);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleEnvioInvalido(MultipartException exception) {
        return ResponseEntity.badRequest().body(Map.of("message",
                "Não foi possível receber os arquivos. Selecione-os novamente e tente enviar."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleErroInesperado(Exception exception) {
        if (exception instanceof ErrorResponse erro && erro.getStatusCode().value() < 500) {
            return ResponseEntity.status(erro.getStatusCode()).body(Map.of("message",
                    "Não foi possível processar a solicitação. Confira o endereço e os dados enviados."));
        }
        logger.error("Erro inesperado ao processar a solicitação", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message",
                "Não foi possível concluir a operação. Tente novamente em instantes."
                + " Se o problema continuar, entre em contato com a equipe responsável."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAcessoNegado(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", RespostaErroSeguranca.MENSAGEM_ACESSO_NEGADO,
                "status", HttpStatus.FORBIDDEN.toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleDadosInvalidos(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("message",
                "Não foi possível ler os dados enviados. Confira os campos e tente novamente."));
    }
}
