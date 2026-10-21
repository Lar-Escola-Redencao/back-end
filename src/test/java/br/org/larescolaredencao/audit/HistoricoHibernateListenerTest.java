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
    void pesquisaGuardaValoresAnterioresSemIncluirNomesDeCampos() {
        String texto = HistoricoHibernateListener.textoPesquisavel(Map.of(
                "campoInterno", "João", "cpf", "123.456.789-00", "aula", "2026-09-01"));
        assertThat(texto).contains("joao", "12345678900", "01/09/2026");
        assertThat(texto).doesNotContain("campointerno", "cpf", "aula");
    }
}
