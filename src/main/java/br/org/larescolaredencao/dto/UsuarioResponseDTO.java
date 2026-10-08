package br.org.larescolaredencao.dto;

import br.org.larescolaredencao.model.ComposicaoFamiliar;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.FichaSocioeconomica;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.PeriodoEscolar;
import br.org.larescolaredencao.model.enums.SerieEscolar;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private StatusMatricula statusMatricula;
    private LocalDateTime dataPrimeiraMatricula;
    private LocalDate dataDesligamento;
    private String justificativaEgresso;
    private boolean matriculaCorrigida;
    private List<ContatoResponseDTO> contatos;
    private List<ComposicaoFamiliarDTO> composicaoFamiliar;
    private FichaSocioeconomicaDTO fichaSocioeconomica;

    public UsuarioResponseDTO(Usuario usuario, List<ContatoUsuario> contatos, Matricula matriculaAtiva) {
        this(usuario, contatos, matriculaAtiva, null, null);
    }

    public UsuarioResponseDTO(Usuario usuario,
                              List<ContatoUsuario> contatos,
                              Matricula matriculaAtiva,
                              List<ComposicaoFamiliar> composicaoFamiliar,
                              FichaSocioeconomica fichaSocioeconomica) {
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
            this.statusMatricula = matriculaAtiva.getStatus();
            this.dataDesligamento = matriculaAtiva.getDataDesligamento();
            this.justificativaEgresso = matriculaAtiva.getJustificativaEgresso();
        }

        if (contatos != null && !contatos.isEmpty()) {
            this.contatos = contatos.stream()
                    .map(ContatoResponseDTO::new)
                    .collect(Collectors.toList());
        }

        if (composicaoFamiliar != null) {
            this.composicaoFamiliar = composicaoFamiliar.stream()
                    .map(this::mapearComposicaoFamiliar)
                    .collect(Collectors.toList());
        }

        if (fichaSocioeconomica != null) {
            this.fichaSocioeconomica = mapearFichaSocioeconomica(fichaSocioeconomica);
        }
    }

    private ComposicaoFamiliarDTO mapearComposicaoFamiliar(ComposicaoFamiliar composicao) {
        ComposicaoFamiliarDTO dto = new ComposicaoFamiliarDTO();
        dto.setNomeCompleto(composicao.getNomeCompleto());
        dto.setParentescoVinculo(composicao.getParentescoVinculo());
        dto.setIdade(composicao.getIdade());
        dto.setEscolaridade(composicao.getEscolaridade());
        dto.setRenda(composicao.getRenda());
        dto.setBeneficios(composicao.getBeneficios());
        return dto;
    }

    private FichaSocioeconomicaDTO mapearFichaSocioeconomica(FichaSocioeconomica ficha) {
        FichaSocioeconomicaDTO dto = new FichaSocioeconomicaDTO();
        dto.setPossuiProblemaSaude(ficha.getPossuiProblemaSaude());
        dto.setDescProblemaSaude(ficha.getDescProblemaSaude());
        dto.setUsaMedicacao(ficha.getUsaMedicacao());
        dto.setDescMedicacao(ficha.getDescMedicacao());
        dto.setTemAlergia(ficha.getTemAlergia());
        dto.setDescAlergia(ficha.getDescAlergia());
        dto.setTipoMoradia(ficha.getTipoMoradia());
        dto.setValorAluguel(ficha.getValorAluguel());
        dto.setValorFinanciamento(ficha.getValorFinanciamento());
        dto.setDespesaEnergia(ficha.getDespesaEnergia());
        dto.setDespesaAgua(ficha.getDespesaAgua());
        dto.setDespesaInternet(ficha.getDespesaInternet());
        dto.setDespesaTelefone(ficha.getDespesaTelefone());
        dto.setDespesaMercado(ficha.getDespesaMercado());
        dto.setDespesaFarmacia(ficha.getDespesaFarmacia());
        dto.setDespesaFinanciamentos(ficha.getDespesaFinanciamentos());
        dto.setDespesaOutras(ficha.getDespesaOutras());
        dto.setUtilizaCarro(ficha.getUtilizaCarro());
        dto.setGastoCarro(ficha.getGastoCarro());
        dto.setUtilizaMoto(ficha.getUtilizaMoto());
        dto.setGastoMoto(ficha.getGastoMoto());
        dto.setUtilizaTransportePublico(ficha.getUtilizaTransportePublico());
        dto.setGastoTransportePublico(ficha.getGastoTransportePublico());
        dto.setUtilizaVan(ficha.getUtilizaVan());
        dto.setGastoVan(ficha.getGastoVan());
        dto.setAndandoOuBicicleta(ficha.getAndandoOuBicicleta());
        dto.setReligiao(ficha.getReligiao());
        return dto;
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

    public StatusMatricula getStatusMatricula() { return statusMatricula; }
    public LocalDateTime getDataPrimeiraMatricula() { return dataPrimeiraMatricula; }
    public void setDataPrimeiraMatricula(LocalDateTime dataPrimeiraMatricula) { this.dataPrimeiraMatricula = dataPrimeiraMatricula; }
    public LocalDate getDataDesligamento() { return dataDesligamento; }
    public String getJustificativaEgresso() { return justificativaEgresso; }
    public boolean isMatriculaCorrigida() { return matriculaCorrigida; }
    public void setMatriculaCorrigida(boolean matriculaCorrigida) { this.matriculaCorrigida = matriculaCorrigida; }

    public List<ContatoResponseDTO> getContatos() {
        return contatos;
    }

    public List<ComposicaoFamiliarDTO> getComposicaoFamiliar() {
        return composicaoFamiliar;
    }

    public FichaSocioeconomicaDTO getFichaSocioeconomica() {
        return fichaSocioeconomica;
    }
}
