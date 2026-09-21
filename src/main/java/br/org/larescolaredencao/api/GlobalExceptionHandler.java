package br.org.larescolaredencao.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException exception) {
        Map<String, String> erro = new LinkedHashMap<>();
        erro.put("message", exception.getReason());
        erro.put("status", exception.getStatusCode().toString());
        return ResponseEntity.status(exception.getStatusCode()).body(erro);
    }
}