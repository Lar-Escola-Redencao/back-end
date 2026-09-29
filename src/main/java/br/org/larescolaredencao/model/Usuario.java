package br.org.larescolaredencao.model;

import br.org.larescolaredencao.model.enums.PeriodoEscolar;
import br.org.larescolaredencao.model.enums.SerieEscolar;
import br.org.larescolaredencao.model.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(unique = true, length = 14)
    private String cpf;

    @Column(name = "documento_auxiliar", length = 100)
    private String documentoAuxiliar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;
    
    @Column(name = "cad_unico", length = 50)
    private String cadUnico;

    @Column(length = 255)
    private String endereco;
    
    @Column(length = 100)
    private String bairro;
    
    @Column(length = 10)
    private String cep;
    
    @Column(length = 150)
    private String escola;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_escolar")
    private PeriodoEscolar periodoEscolar;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "serie_escolar")
    private SerieEscolar serieEscolar;
    
    @Column(name = "ra_escolar", length = 20)
    private String raEscolar;

    @Column(name = "imagem_perfil", length = 255)
    private String imagemPerfil;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getDocumentoAuxiliar() {
        return documentoAuxiliar;
    }

    public void setDocumentoAuxiliar(String documentoAuxiliar) {
        this.documentoAuxiliar = documentoAuxiliar;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getCadUnico() {
        return cadUnico;
    }

    public void setCadUnico(String cadUnico) {
        this.cadUnico = cadUnico;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getEscola() {
        return escola;
    }

    public void setEscola(String escola) {
        this.escola = escola;
    }

    public PeriodoEscolar getPeriodoEscolar() {
        return periodoEscolar;
    }

    public void setPeriodoEscolar(PeriodoEscolar periodoEscolar) {
        this.periodoEscolar = periodoEscolar;
    }

    public SerieEscolar getSerieEscolar() {
        return serieEscolar;
    }

    public void setSerieEscolar(SerieEscolar serieEscolar) {
        this.serieEscolar = serieEscolar;
    }

    public String getRaEscolar() {
        return raEscolar;
    }

    public void setRaEscolar(String raEscolar) {
        this.raEscolar = raEscolar;
    }

    public String getImagemPerfil() {
        return imagemPerfil;
    }

    public void setImagemPerfil(String imagemPerfil) {
        this.imagemPerfil = imagemPerfil;
    }
}