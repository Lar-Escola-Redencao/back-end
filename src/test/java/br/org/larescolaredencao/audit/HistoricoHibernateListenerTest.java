package br.org.larescolaredencao.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.Map;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import br.org.larescolaredencao.model.Membro;

class HistoricoHibernateListenerTest {

    @Test
    void trocaSomenteDeSenhaNaoCriaEventoNemAcessaConexao() {
        var persister = mock(EntityPersister.class);
        var sessao = mock(EventSource.class);
        var tipoTexto = mock(Type.class);
        doReturn(Membro.class).when(persister).getMappedClass();
        when(persister.getPropertyNames()).thenReturn(new String[]{"nomeCompleto", "senha"});
        when(persister.getPropertyTypes()).thenReturn(new Type[]{tipoTexto, tipoTexto});
        var evento = new PostUpdateEvent(new Membro(), 7,
                new Object[]{"Ana", "novo-hash"}, new Object[]{"Ana", "hash-antigo"},
                new int[]{1}, persister, sessao);
        new HistoricoHibernateListener().onPostUpdate(evento);
        verifyNoInteractions(sessao);
    }

    @Test
    void trocaSomenteDaMascaraDoTelefoneNaoCriaEvento() {
        var persister = mock(EntityPersister.class);
        var sessao = mock(EventSource.class);
        var tipoTexto = mock(Type.class);
        doReturn(br.org.larescolaredencao.model.Contato.class).when(persister).getMappedClass();
        when(persister.getPropertyNames()).thenReturn(new String[]{"telefone"});
        when(persister.getPropertyTypes()).thenReturn(new Type[]{tipoTexto});
        var evento = new PostUpdateEvent(new br.org.larescolaredencao.model.Contato(), 1,
                new Object[]{"16991111111"}, new Object[]{"(16) 99111-1111"},
                new int[]{0}, persister, sessao);

        new HistoricoHibernateListener().onPostUpdate(evento);

        verifyNoInteractions(sessao);
    }

    @Test
    void valoresVaziosENumerosEquivalentesNaoCriamEvento() {
        verificarAtualizacaoEquivalente("cep", null, "");
        verificarAtualizacaoEquivalente("cep", "", "   ");
        verificarAtualizacaoEquivalente("renda", new java.math.BigDecimal("100.00"),
                new java.math.BigDecimal("100.0"));
    }

    private void verificarAtualizacaoEquivalente(String campo, Object anterior, Object novo) {
        var persister = mock(EntityPersister.class);
        var sessao = mock(EventSource.class);
        var tipo = mock(Type.class);
        doReturn(br.org.larescolaredencao.model.Usuario.class).when(persister).getMappedClass();
        when(persister.getPropertyNames()).thenReturn(new String[]{campo});
        when(persister.getPropertyTypes()).thenReturn(new Type[]{tipo});
        var evento = new PostUpdateEvent(new br.org.larescolaredencao.model.Usuario(), 1,
                new Object[]{novo}, new Object[]{anterior}, new int[]{0}, persister, sessao);

        new HistoricoHibernateListener().onPostUpdate(evento);

        verifyNoInteractions(sessao);
    }

    @Test
    void remocaoRealDeValorContinuaRegistrandoEvento() {
        var persister = mock(EntityPersister.class);
        var sessao = mock(EventSource.class);
        var tipo = mock(Type.class);
        doReturn(br.org.larescolaredencao.model.Contato.class).when(persister).getMappedClass();
        when(persister.getPropertyNames()).thenReturn(new String[]{"localTrabalho"});
        when(persister.getPropertyTypes()).thenReturn(new Type[]{tipo});
        var evento = new PostUpdateEvent(new br.org.larescolaredencao.model.Contato(), 1,
                new Object[]{""}, new Object[]{"Escola"}, new int[]{0}, persister, sessao);

        new HistoricoHibernateListener().onPostUpdate(evento);

        org.mockito.Mockito.verify(sessao).doWork(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void pesquisaGuardaValoresAnterioresSemIncluirNomesDeCampos() {
        String texto = HistoricoHibernateListener.textoPesquisavel(Map.of(
                "campoInterno", "João", "cpf", "123.456.789-00", "aula", "2026-09-01"));
        assertThat(texto).contains("joao", "12345678900", "01/09/2026");
        assertThat(texto).doesNotContain("campointerno", "cpf", "aula");
    }

    @Test
    void recriarMesmosVinculosRemoveEventoIntermediarioDeUnidadesVazias() throws Exception {
        var sessao = mock(EventSource.class);
        var conexao = mock(java.sql.Connection.class);
        var consulta = mock(java.sql.PreparedStatement.class);
        var gravacao = mock(java.sql.PreparedStatement.class);
        var remocao = mock(java.sql.PreparedStatement.class);
        var resultado = mock(java.sql.ResultSet.class);
        var colecao = mock(org.hibernate.collection.spi.PersistentCollection.class);
        var antes = mock(org.hibernate.event.spi.PreCollectionRemoveEvent.class);
        var removido = mock(org.hibernate.event.spi.PostCollectionRemoveEvent.class);
        var recriado = mock(org.hibernate.event.spi.PostCollectionRecreateEvent.class);
        Membro membro = new Membro();
        membro.setNomeCompleto("Monitor");

        for (var evento : java.util.List.of(antes, removido, recriado)) {
            when(evento.getAffectedOwnerOrNull()).thenReturn(membro);
            when(evento.getAffectedOwnerIdOrNull()).thenReturn(3);
            when(evento.getCollection()).thenReturn(colecao);
            when(evento.getSession()).thenReturn(sessao);
        }
        when(colecao.getRole()).thenReturn("br.org.larescolaredencao.model.Membro.unidades");
        when(conexao.prepareStatement(org.mockito.ArgumentMatchers.startsWith("select"))).thenReturn(consulta);
        when(conexao.prepareStatement(org.mockito.ArgumentMatchers.startsWith("insert"))).thenReturn(gravacao);
        when(conexao.prepareStatement(org.mockito.ArgumentMatchers.startsWith("delete"))).thenReturn(remocao);
        when(consulta.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(true, false, false, true, false);
        when(resultado.getInt("id")).thenReturn(1);
        when(resultado.getString("nome")).thenReturn("Sede São José");
        org.mockito.Mockito.doAnswer(chamada -> {
            chamada.getArgument(0, org.hibernate.jdbc.Work.class).execute(conexao);
            return null;
        }).when(sessao).doWork(org.mockito.ArgumentMatchers.any());

        var listener = new HistoricoHibernateListener();
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            listener.onPreRemoveCollection(antes);
            listener.onPostRemoveCollection(removido);
            listener.onPostRecreateCollection(recriado);

            org.mockito.Mockito.verify(gravacao).executeUpdate();
            org.mockito.Mockito.verify(remocao).executeUpdate();
        } finally {
            var sincronizacoes = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            sincronizacoes.forEach(sincronizacao -> sincronizacao.afterCompletion(0));
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
