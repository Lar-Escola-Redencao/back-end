package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

public class CriarRedeSocialDTO {
	private String nome;
	private String url;
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
