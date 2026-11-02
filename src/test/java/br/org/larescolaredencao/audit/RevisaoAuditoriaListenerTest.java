package br.org.larescolaredencao.audit;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.RevisaoAuditoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RevisaoAuditoriaListenerTest {

    @BeforeEach
    void iniciarSincronizacao() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void encerrarSincronizacao() {
        for (TransactionSynchronization sincronizacao : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    @Test
    void usaIdentificacaoDoResponsavelAutenticadoSemCopiarCredenciais() {
        Membro membro = new Membro();
        membro.setId(19);
        membro.setNomeCompleto("Administradora");
        membro.setSenha("hash-fora-do-historico");

        var autenticacao = new UsernamePasswordAuthenticationToken(membro, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(autenticacao);

        RevisaoAuditoria revisao = new RevisaoAuditoria();
        new RevisaoAuditoriaListener().newRevision(revisao);

        assertThat(revisao.getIdMembro()).isEqualTo(19);
        assertThat(revisao.getNomeResponsavel()).isEqualTo("Administradora");
        assertThat(revisao.getOperacao()).isEqualTo(OperacaoAuditoria.atual().id);
        assertThat(revisao.isReferenciaImplantacao()).isFalse();
    }

    @Test
    void rollbackNaoReaproveitaIdentificadorNaProximaTransacao() {
        String primeiraOperacao = OperacaoAuditoria.atual().id;

        for (TransactionSynchronization sincronizacao : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();

        assertThat(OperacaoAuditoria.atual().id).isNotEqualTo(primeiraOperacao);
    }

    @Test
    void operacaoAutomaticaNaoInventaColaborador() {
        RevisaoAuditoria revisao = new RevisaoAuditoria();
        new RevisaoAuditoriaListener().newRevision(revisao);

        assertThat(revisao.getIdMembro()).isNull();
        assertThat(revisao.getNomeResponsavel()).isEqualTo("Sistema (operação automática)");
    }
}
