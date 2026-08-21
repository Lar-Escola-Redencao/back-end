package br.org.larescolaredencao.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ProblemDetail tratarApiException(ApiException ex) {
		if (ex.getStatus().is5xxServerError()) {
			logger.error("Erro de servidor", ex);
		} else {
			logger.info("Erro de negócio: {}", ex.getMessage());
		}
		return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail tratarErroInesperado(Exception ex) {
		logger.error("Erro inesperado", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Erro interno ao processar a solicitação.");
	}
}
