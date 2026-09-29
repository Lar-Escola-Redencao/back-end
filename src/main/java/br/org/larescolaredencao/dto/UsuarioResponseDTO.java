package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.enums.PeriodoEscolar;
import br.org.larescolaredencao.model.enums.SerieEscolar;
import br.org.larescolaredencao.model.enums.TipoDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UsuarioResponseDTO {

    private Integer id;
    private String nomeCompleto;
    private LocalDate dataNascimento;
    private String cpf;
    private String documentoAuxiliar;
    private TipoDocumento tipoDocumento;
    private String cadUnico;
    private String endereco;
    private String bairro;
    private String cep;
    private String escola;
    private PeriodoEscolar periodoEscolar;
    private SerieEscolar serieEscolar;
    private String raEscolar;
    private String imagemPerfil;
    private Integer idTurma;
    private String nomeTurma;
    private Integer idUnidade;
    private String nomeUnidade;
    private List<ContatoResponseDTO> contatos;

    public UsuarioResponseDTO(Usuario usuario, List<ContatoUsuario> contatos, Matricula matriculaAtiva) {
        this.id = usuario.getId();
        this.nomeCompleto = usuario.getNomeCompleto();
        this.dataNascimento = usuario.getDataNascimento();
        this.cpf = usuario.getCpf();
        this.documentoAuxiliar = usuario.getDocumentoAuxiliar();
        this.tipoDocumento = usuario.getTipoDocumento();
        this.cadUnico = usuario.getCadUnico();
        this.endereco = usuario.getEndereco();
        this.bairro = usuario.getBairro();
        this.cep = usuario.getCep();
        this.escola = usuario.getEscola();
        this.periodoEscolar = usuario.getPeriodoEscolar();
        this.serieEscolar = usuario.getSerieEscolar();
        this.raEscolar = usuario.getRaEscolar();
        this.imagemPerfil = usuario.getImagemPerfil();

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

    public String getCadUnico() {
        return cadUnico;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCep() {
        return cep;
    }

    public String getEscola() {
        return escola;
    }

    public PeriodoEscolar getPeriodoEscolar() {
        return periodoEscolar;
    }

    public SerieEscolar getSerieEscolar() {
        return serieEscolar;
    }

    public String getRaEscolar() {
        return raEscolar;
    }

    public String getImagemPerfil() {
        return imagemPerfil;
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