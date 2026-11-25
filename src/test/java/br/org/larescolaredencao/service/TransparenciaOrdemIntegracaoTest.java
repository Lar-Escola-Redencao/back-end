package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.CriarSecaoDTO;
import br.org.larescolaredencao.dto.ReordenarSecaoDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Banco exclusivo em memória: testa JPA e transações reais sem tocar no MySQL de desenvolvimento. */
@SpringJUnitConfig(TransparenciaOrdemIntegracaoTest.Config.class)
class TransparenciaOrdemIntegracaoTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean static org.springframework.core.convert.ConversionService conversionService() {
            return org.springframework.boot.convert.ApplicationConversionService.getSharedInstance();
        }
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:transparencia_ordem;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var f = new LocalContainerEntityManagerFactoryBean();
            f.setDataSource(ds); f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setManagedTypes(PersistenceManagedTypes.of(Pagina.class.getName(), Secao.class.getName(), Documento.class.getName()));
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.integration.envers.enabled", "false"));
            return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f) { return new JpaTransactionManager(f); }
        @Bean EntityManager em(EntityManagerFactory f) { return SharedEntityManagerCreator.createSharedEntityManager(f); }
        @Bean PaginaRepository paginas(EntityManager em) { return new JpaRepositoryFactory(em).getRepository(PaginaRepository.class); }
        @Bean SecaoRepository secoes(EntityManager em) { return new JpaRepositoryFactory(em).getRepository(SecaoRepository.class); }
        @Bean DocumentoRepository documentos(EntityManager em) { return new JpaRepositoryFactory(em).getRepository(DocumentoRepository.class); }
        @Bean ArquivoService arquivos() {
            var arquivos = mock(ArquivoService.class);
            when(arquivos.sanitizarTexto(anyString())).thenAnswer(i -> i.getArgument(0));
            return arquivos;
        }
        @Bean PaginaService service(PaginaRepository p, SecaoRepository s, DocumentoRepository d, ArquivoService a) {
            return new PaginaService(p, s, d, a);
        }
    }

    @Autowired PaginaService service;
    @Autowired PaginaRepository paginas;
    @Autowired SecaoRepository secoes;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired DataSource ds;
    long a, b;

    @BeforeEach void preparar() {
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("delete from documento"); jdbc.update("delete from secao");
        jdbc.update("merge into pagina (id, nome, ativo) key(id) values (1, 'Transparência', true)");
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            a = criar("Financeiro"); b = criar("Atas");
        });
    }
    private long criar(String titulo) {
        var dto = new CriarSecaoDTO(); dto.setTitulo(titulo);
        return service.criarSecao(1L, dto).getId();
    }
    private ReordenarSecaoDTO posicao(long id, int ordem) {
        var dto = new ReordenarSecaoDTO(); dto.setId(id); dto.setOrdem(ordem); return dto;
    }
    private List<ReordenarSecaoDTO> inversa() { return List.of(posicao(b, 1), posicao(a, 2)); }
    private List<Long> ids() { return service.listarSecoes(1L).stream().map(Secao::getId).toList(); }

    @Test void excluirSecaoIntermediariaFechaALacunaNaOrdemSalva() {
        long c = criar("Estatuto");
        service.deletarSecao(b);
        assertThat(ids()).containsExactly(a, c);
        assertThat(service.listarSecoes(1L)).extracting(Secao::getOrdem).containsExactly(1, 2);
    }

    @Test void excluirPrimeiraSecaoRenumeraTodasAsSeguintes() {
        long c = criar("Estatuto");
        service.deletarSecao(a);
        assertThat(ids()).containsExactly(b, c);
        assertThat(service.listarSecoes(1L)).extracting(Secao::getOrdem).containsExactly(1, 2);
    }

    @Test void falhaReverteExclusaoERenumeracaoJuntas() {
        long c = criar("Estatuto");
        assertThatThrownBy(() -> new TransactionTemplate(tm).executeWithoutResult(tx -> {
            service.deletarSecao(a); em.flush();
            throw new IllegalStateException("Falha após exclusão");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(ids()).containsExactly(a, b, c);
        assertThat(service.listarSecoes(1L)).extracting(Secao::getOrdem).containsExactly(1, 2, 3);
    }

    @Test void persisteAOrdemEReverteTudoQuandoATransacaoFalha() {
        assertThatThrownBy(() -> new TransactionTemplate(tm).executeWithoutResult(tx -> {
            service.reordenarSecoes(inversa()); em.flush();
            throw new IllegalStateException("Falha após gravar");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(ids()).containsExactly(a, b);
        service.reordenarSecoes(inversa());
        assertThat(ids()).containsExactly(b, a);
    }

    @Test void duasReordenacoesSimultaneasProduzemUmaPermutacaoCompleta() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var largada = new CountDownLatch(1);
            var primeira = executor.submit(() -> { largada.await(); service.reordenarSecoes(inversa()); return true; });
            var segunda = executor.submit(() -> { largada.await(); service.reordenarSecoes(List.of(posicao(a, 1), posicao(b, 2))); return true; });
            largada.countDown(); primeira.get(10, TimeUnit.SECONDS); segunda.get(10, TimeUnit.SECONDS);
            assertThat(List.of(List.of(a, b), List.of(b, a))).contains(ids());
            assertThat(service.listarSecoes(1L)).extracting(Secao::getOrdem).containsExactly(1, 2);
        }
    }

    @Test void criacaoAguardaReordenacaoEEntraAoFinal() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var iniciou = new CountDownLatch(1);
            var futuro = new java.util.concurrent.atomic.AtomicReference<Future<Long>>();
            new TransactionTemplate(tm).executeWithoutResult(tx -> {
                paginas.findByIdComBloqueio(1L);
                futuro.set(executor.submit(() -> { iniciou.countDown(); return criar("Estatuto"); }));
                try { assertThat(iniciou.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
                service.reordenarSecoes(inversa());
            });
            long nova = futuro.get().get(10, TimeUnit.SECONDS);
            assertThat(ids()).containsExactly(b, a, nova);
            assertThat(service.listarSecoes(1L)).extracting(Secao::getOrdem).containsExactly(1, 2, 3);
        }
    }
}
