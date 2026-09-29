package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.enums.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class AtualizarAssistidoDTO {

    @NotBlank
    private String nomeCompleto;

    @NotNull
    private LocalDate dataNascimento;

    private String cpf;

    private String documentoAuxiliar;

    private TipoDocumento tipoDocumento;

    private String endereco;

    @NotNull
    private Integer idTurma;

    @Valid
    @NotNull
    @Size(min = 1, max = 4, message = "O assistido deve ter entre 1 e 4 contatos/responsáveis.")
    private List<ContatoDTO> contatos;

    @AssertTrue(message = "É obrigatório informar o CPF/CIN ou um documento auxiliar (Certidão de Nascimento/Outro).")
    public boolean isDocumentoInformado() {
        boolean temCpf = this.cpf != null && !this.cpf.isBlank();
        boolean temDocumentoAuxiliar = this.documentoAuxiliar != null && !this.documentoAuxiliar.isBlank() && this.tipoDocumento != null;
        
        return temCpf || temDocumentoAuxiliar;
    }

    @AssertTrue(message = "Deve haver exatamente um responsável marcado como principal.")
    public boolean isApenasUmContatoPrincipal() {
        if (contatos == null || contatos.isEmpty()) {
            return true; 
        }
        
        long principais = contatos.stream()
                .filter(c -> Boolean.TRUE.equals(c.getPrincipal()))
                .count();
                
        return principais == 1;
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

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Integer getIdTurma() {
        return idTurma;
    }

    public void setIdTurma(Integer idTurma) {
        this.idTurma = idTurma;
    }

    public List<ContatoDTO> getContatos() {
        return contatos;
    }

    public void setContatos(List<ContatoDTO> contatos) {
        this.contatos = contatos;
    }
}