package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.enums.TipoDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AssistidoResponseDTO {

    private Integer id;
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String cpf;
    private String documentoAuxiliar;
    private TipoDocumento tipoDocumento;
    private String endereco;
    private List<ContatoResponseDTO> contatos;

    public AssistidoResponseDTO(Assistido assistido) {
        this.id = assistido.getId();
        this.nomeCompleto = assistido.getNomeCompleto();
        this.dataNascimento = assistido.getDataNascimento();
        this.cpf = assistido.getCpf();
        this.documentoAuxiliar = assistido.getDocumentoAuxiliar();
        this.tipoDocumento = assistido.getTipoDocumento();
        this.endereco = assistido.getEndereco();
    }

    public AssistidoResponseDTO(Assistido assistido, List<ContatoAssistido> contatos) {
        this(assistido);
        if (contatos != null && !contatos.isEmpty()) {
            this.contatos = contatos.stream()
                    .map(ContatoResponseDTO::new)
                    .collect(Collectors.toList());
        }
    }

    public Integer getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCpf() {
        return cpf;
    }

    public String getDocumentoAuxiliar() {
        return documentoAuxiliar;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getEndereco() {
        return endereco;
    }

    public List<ContatoResponseDTO> getContatos() {
        return contatos;
    }
}