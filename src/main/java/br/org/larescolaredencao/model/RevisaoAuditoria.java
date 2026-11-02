package br.org.larescolaredencao.model;

import br.org.larescolaredencao.audit.RevisaoAuditoriaListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity(RevisaoAuditoriaListener.class)
public class RevisaoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Integer id;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private long instante;

    @Column(name = "id_membro")
    private Integer idMembro;

    @Column(name = "nome_responsavel", length = 150)
    private String nomeResponsavel;

    @Column(name = "operacao", length = 36, unique = true)
    private String operacao;

    @Column(name = "referencia_implantacao", nullable = false)
    private boolean referenciaImplantacao;

    public Integer getId() {
        return id;
    }

    public long getInstante() {
        return instante;
    }

    public Integer getIdMembro() {
        return idMembro;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public String getOperacao() {
        return operacao;
    }

    public boolean isReferenciaImplantacao() {
        return referenciaImplantacao;
    }

    public void setIdMembro(Integer idMembro) {
        this.idMembro = idMembro;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }
}
