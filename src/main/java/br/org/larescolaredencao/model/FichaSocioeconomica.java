package br.org.larescolaredencao.model;

import br.org.larescolaredencao.model.enums.TipoMoradia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "ficha_socioeconomica")
public class FichaSocioeconomica {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;
    
    private Boolean possuiProblemaSaude = false;
    
    @Column(length = 255)
    private String descProblemaSaude;
    
    private Boolean usaMedicacao = false;
    
    @Column(length = 255)
    private String descMedicacao;
    
    private Boolean temAlergia = false;
    
    @Column(length = 255)
    private String descAlergia;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMoradia tipoMoradia;
    
    private BigDecimal valorAluguel = BigDecimal.ZERO;
    private BigDecimal valorFinanciamento = BigDecimal.ZERO;
    
    private BigDecimal despesaEnergia = BigDecimal.ZERO;
    private BigDecimal despesaAgua = BigDecimal.ZERO;
    private BigDecimal despesaInternet = BigDecimal.ZERO;
    private BigDecimal despesaTelefone = BigDecimal.ZERO;
    private BigDecimal despesaMercado = BigDecimal.ZERO;
    private BigDecimal despesaFarmacia = BigDecimal.ZERO;
    private BigDecimal despesaFinanciamentos = BigDecimal.ZERO;
    private BigDecimal despesaOutras = BigDecimal.ZERO;
    
    private Boolean utilizaCarro = false;
    private BigDecimal gastoCarro = BigDecimal.ZERO;
    
    private Boolean utilizaMoto = false;
    private BigDecimal gastoMoto = BigDecimal.ZERO;
    
    private Boolean utilizaTransportePublico = false;
    private BigDecimal gastoTransportePublico = BigDecimal.ZERO;
    
    private Boolean utilizaVan = false;
    private BigDecimal gastoVan = BigDecimal.ZERO;
    
    private Boolean andandoOuBicicleta = false;
    
    @Column(length = 100)
    private String religiao;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Boolean getPossuiProblemaSaude() {
        return possuiProblemaSaude;
    }

    public void setPossuiProblemaSaude(Boolean possuiProblemaSaude) {
        this.possuiProblemaSaude = possuiProblemaSaude;
    }

    public String getDescProblemaSaude() {
        return descProblemaSaude;
    }

    public void setDescProblemaSaude(String descProblemaSaude) {
        this.descProblemaSaude = descProblemaSaude;
    }

    public Boolean getUsaMedicacao() {
        return usaMedicacao;
    }

    public void setUsaMedicacao(Boolean usaMedicacao) {
        this.usaMedicacao = usaMedicacao;
    }

    public String getDescMedicacao() {
        return descMedicacao;
    }

    public void setDescMedicacao(String descMedicacao) {
        this.descMedicacao = descMedicacao;
    }

    public Boolean getTemAlergia() {
        return temAlergia;
    }

    public void setTemAlergia(Boolean temAlergia) {
        this.temAlergia = temAlergia;
    }

    public String getDescAlergia() {
        return descAlergia;
    }

    public void setDescAlergia(String descAlergia) {
        this.descAlergia = descAlergia;
    }

    public TipoMoradia getTipoMoradia() {
        return tipoMoradia;
    }

    public void setTipoMoradia(TipoMoradia tipoMoradia) {
        this.tipoMoradia = tipoMoradia;
    }

    public BigDecimal getValorAluguel() {
        return valorAluguel;
    }

    public void setValorAluguel(BigDecimal valorAluguel) {
        this.valorAluguel = valorAluguel;
    }

    public BigDecimal getValorFinanciamento() {
        return valorFinanciamento;
    }

    public void setValorFinanciamento(BigDecimal valorFinanciamento) {
        this.valorFinanciamento = valorFinanciamento;
    }

    public BigDecimal getDespesaEnergia() {
        return despesaEnergia;
    }

    public void setDespesaEnergia(BigDecimal despesaEnergia) {
        this.despesaEnergia = despesaEnergia;
    }

    public BigDecimal getDespesaAgua() {
        return despesaAgua;
    }

    public void setDespesaAgua(BigDecimal despesaAgua) {
        this.despesaAgua = despesaAgua;
    }

    public BigDecimal getDespesaInternet() {
        return despesaInternet;
    }

    public void setDespesaInternet(BigDecimal despesaInternet) {
        this.despesaInternet = despesaInternet;
    }

    public BigDecimal getDespesaTelefone() {
        return despesaTelefone;
    }

    public void setDespesaTelefone(BigDecimal despesaTelefone) {
        this.despesaTelefone = despesaTelefone;
    }

    public BigDecimal getDespesaMercado() {
        return despesaMercado;
    }

    public void setDespesaMercado(BigDecimal despesaMercado) {
        this.despesaMercado = despesaMercado;
    }

    public BigDecimal getDespesaFarmacia() {
        return despesaFarmacia;
    }

    public void setDespesaFarmacia(BigDecimal despesaFarmacia) {
        this.despesaFarmacia = despesaFarmacia;
    }

    public BigDecimal getDespesaFinanciamentos() {
        return despesaFinanciamentos;
    }

    public void setDespesaFinanciamentos(BigDecimal despesaFinanciamentos) {
        this.despesaFinanciamentos = despesaFinanciamentos;
    }

    public BigDecimal getDespesaOutras() {
        return despesaOutras;
    }

    public void setDespesaOutras(BigDecimal despesaOutras) {
        this.despesaOutras = despesaOutras;
    }

    public Boolean getUtilizaCarro() {
        return utilizaCarro;
    }

    public void setUtilizaCarro(Boolean utilizaCarro) {
        this.utilizaCarro = utilizaCarro;
    }

    public BigDecimal getGastoCarro() {
        return gastoCarro;
    }

    public void setGastoCarro(BigDecimal gastoCarro) {
        this.gastoCarro = gastoCarro;
    }

    public Boolean getUtilizaMoto() {
        return utilizaMoto;
    }

    public void setUtilizaMoto(Boolean utilizaMoto) {
        this.utilizaMoto = utilizaMoto;
    }

    public BigDecimal getGastoMoto() {
        return gastoMoto;
    }

    public void setGastoMoto(BigDecimal gastoMoto) {
        this.gastoMoto = gastoMoto;
    }

    public Boolean getUtilizaTransportePublico() {
        return utilizaTransportePublico;
    }

    public void setUtilizaTransportePublico(Boolean utilizaTransportePublico) {
        this.utilizaTransportePublico = utilizaTransportePublico;
    }

    public BigDecimal getGastoTransportePublico() {
        return gastoTransportePublico;
    }

    public void setGastoTransportePublico(BigDecimal gastoTransportePublico) {
        this.gastoTransportePublico = gastoTransportePublico;
    }

    public Boolean getUtilizaVan() {
        return utilizaVan;
    }

    public void setUtilizaVan(Boolean utilizaVan) {
        this.utilizaVan = utilizaVan;
    }

    public BigDecimal getGastoVan() {
        return gastoVan;
    }

    public void setGastoVan(BigDecimal gastoVan) {
        this.gastoVan = gastoVan;
    }

    public Boolean getAndandoOuBicicleta() {
        return andandoOuBicicleta;
    }

    public void setAndandoOuBicicleta(Boolean andandoOuBicicleta) {
        this.andandoOuBicicleta = andandoOuBicicleta;
    }

    public String getReligiao() {
        return religiao;
    }

    public void setReligiao(String religiao) {
        this.religiao = religiao;
    }
}