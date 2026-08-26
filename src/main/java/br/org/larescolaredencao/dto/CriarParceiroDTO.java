package br.org.larescolaredencao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CriarParceiroDTO {
	@NotBlank
	@Size(min = 3, max = 50)
	private String nome;

	@NotBlank
	private String logo;

	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getLogo() {
		return logo;
	}
	public void setLogo(String logo) {
		this.logo = logo;
	}
}
