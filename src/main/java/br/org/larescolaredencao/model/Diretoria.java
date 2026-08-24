package br.org.larescolaredencao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Diretoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 150)
	private String nome;

	@NotBlank
	@Size(max = 100)
	private String cargo;

	@Size(max = 255)
	private String foto;

	private Boolean ativo;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
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
	public String getFoto() {
		return foto;
	}
	public void setFoto(String foto) {
		this.foto = foto;
	}
	public Boolean getAtivo() {
		return ativo;
	}
	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}
}
