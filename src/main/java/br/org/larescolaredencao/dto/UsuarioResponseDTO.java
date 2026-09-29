package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Assistido;
import br.org.larescolaredencao.model.ContatoAssistido;
import br.org.larescolaredencao.model.Matricula;
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
    private Integer idTurma;
    private String nomeTurma;
    private Integer idUnidade;
    private String nomeUnidade;
    private List<ContatoResponseDTO> contatos;

    public AssistidoResponseDTO(Assistido assistido, List<ContatoAssistido> contatos, Matricula matriculaAtiva) {
        this.id = assistido.getId();
        this.nomeCompleto = assistido.getNomeCompleto();
        this.dataNascimento = assistido.getDataNascimento();
        this.cpf = assistido.getCpf();
        this.documentoAuxiliar = assistido.getDocumentoAuxiliar();
        this.tipoDocumento = assistido.getTipoDocumento();
        this.endereco = assistido.getEndereco();

        if (matriculaAtiva != null) {
            this.idTurma = matriculaAtiva.getTurma().getId();
            this.nomeTurma = matriculaAtiva.getTurma().getPeriodo() + " · " + 
                             matriculaAtiva.getTurma().getHoraInicio() + "–" + 
                             matriculaAtiva.getTurma().getHoraFim();
            this.idUnidade = matriculaAtiva.getTurma().getUnidade().getId();
            this.nomeUnidade = matriculaAtiva.getTurma().getUnidade().getNome();
        }

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
    
    public Integer getIdTurma() {
        return idTurma;
    }

    public String getNomeTurma() {
        return nomeTurma;
    }

    public Integer getIdUnidade() {
        return idUnidade;
    }

    public String getNomeUnidade() {
        return nomeUnidade;
    }

    public List<ContatoResponseDTO> getContatos() {
        return contatos;
    }
}