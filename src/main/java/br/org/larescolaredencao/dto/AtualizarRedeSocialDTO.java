package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

public class AtualizarRedeSocialDTO {
	private String nome;
	private String url;
	private MultipartFile icone;
	private Boolean ativo;

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
	public Boolean getAtivo() {
		return ativo;
	}
	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}
}
