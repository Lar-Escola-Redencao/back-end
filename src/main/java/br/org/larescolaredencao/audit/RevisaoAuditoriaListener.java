package br.org.larescolaredencao.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.RevisaoAuditoria;

public class RevisaoAuditoriaListener implements RevisionListener {

    @Override
    public void newRevision(Object entidade) {
        RevisaoAuditoria revisao = (RevisaoAuditoria) entidade;
        revisao.setOperacao(OperacaoAuditoria.atual().id);
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao != null && autenticacao.isAuthenticated()
                && autenticacao.getPrincipal() instanceof Membro membro) {
            revisao.setIdMembro(membro.getId());
            revisao.setNomeResponsavel(membro.getNomeCompleto());
        } else {
            revisao.setNomeResponsavel("Sistema (operação automática)");
        }
    }
}
