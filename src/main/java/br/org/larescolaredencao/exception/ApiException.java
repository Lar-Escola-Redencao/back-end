package br.org.larescolaredencao.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public ApiException(HttpStatus status, String mensagem, Throwable causa) {
		super(mensagem, causa);
		this.status = status;
	}

	public static ApiException naoEncontrado(String mensagem) {
		return new ApiException(HttpStatus.NOT_FOUND, mensagem);
	}

	public static ApiException conflito(String mensagem) {
		return new ApiException(HttpStatus.CONFLICT, mensagem);
	}

	public HttpStatus getStatus() {
		return status;
	}
}
