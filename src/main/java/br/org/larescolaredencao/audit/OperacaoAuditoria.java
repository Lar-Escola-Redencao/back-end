package br.org.larescolaredencao.audit;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class OperacaoAuditoria {

    private static final Object CHAVE_TRANSACAO = new Object();

    public final String id = UUID.randomUUID().toString();
    final Map<String, Alteracao> alteracoes = new LinkedHashMap<>();
    final Map<Integer, Map<String, Object>> unidadesAnteriores = new LinkedHashMap<>();

    record Alteracao(
            Map<String, Object> anterior,
            Map<String, Object> contextoAnterior) {
    }

    private OperacaoAuditoria() {
    }

    public static OperacaoAuditoria atual() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("A auditoria exige uma transação ativa.");
        }

        OperacaoAuditoria operacaoAtual =
                (OperacaoAuditoria) TransactionSynchronizationManager.getResource(CHAVE_TRANSACAO);
        if (operacaoAtual != null) {
            return operacaoAtual;
        }

        OperacaoAuditoria novaOperacao = new OperacaoAuditoria();
        TransactionSynchronizationManager.bindResource(CHAVE_TRANSACAO, novaOperacao);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void suspend() {
                TransactionSynchronizationManager.unbindResourceIfPossible(CHAVE_TRANSACAO);
            }

            @Override
            public void resume() {
                TransactionSynchronizationManager.bindResource(CHAVE_TRANSACAO, novaOperacao);
            }

            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResourceIfPossible(CHAVE_TRANSACAO);
            }
        });

        return novaOperacao;
    }
}
