package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

public class AtualizarParceiroDTO {
	private String nome;
	private MultipartFile logo;
	private Boolean ativo;

	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public MultipartFile getLogo() {
		return logo;
	}
	public void setLogo(MultipartFile logo) {
		this.logo = logo;
	}
	public Boolean getAtivo() {
		return ativo;
	}
	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}
}
