package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriarRedeSocialDTO {
	@NotBlank
	@Size(max = 50)
	private String nome;

	@NotBlank
	@Size(max = 255)
	private String url;

	@NotNull
	private MultipartFile icone;

	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public MultipartFile getIcone() {
		return icone;
	}
	public void setIcone(MultipartFile icone) {
		this.icone = icone;
	}
}
