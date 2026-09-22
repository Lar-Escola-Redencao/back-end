package br.org.larescolaredencao.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fieldErrors);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, String>> handleOrdenacaoInvalida(PropertyReferenceException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("sort", "Campo de ordenação inválido: " + exception.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleArquivoMuitoGrande(MaxUploadSizeExceededException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("arquivo", "Arquivo excede o tamanho máximo permitido.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }
}
