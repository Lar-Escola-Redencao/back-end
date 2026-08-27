package br.org.larescolaredencao.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriarDiretoriaDTO {

	@NotBlank
	@Size(max = 150)
	private String nome;

	@NotBlank
	@Size(max = 100)
	private String cargo;

	@NotNull
	private MultipartFile foto;

	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getCargo() {
		return cargo;
	}
	public void setCargo(String cargo) {
		this.cargo = cargo;
	}
	public MultipartFile getFoto() {
		return foto;
	}
	public void setFoto(MultipartFile foto) {
		this.foto = foto;
	}
}
