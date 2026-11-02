package br.org.larescolaredencao.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Unidade;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "LER_TEST_MYSQL", matches = "true")
class MembroRepositoryTest {
    @Autowired private MembroRepository membros;
    @Autowired private EntityManager entityManager;

    private Unidade unidade;
    private Unidade outraUnidade;
    private Papel administrador;
    private Papel monitor;
    private Membro globalSemVinculo;
    private Membro globalComVinculos;
    private Membro monitorLocal;
    private Membro monitorExterno;
    private String prefixo;
    private long totalAnterior;

    @BeforeEach
    void preparar() {
        prefixo = "Filtro " + UUID.randomUUID();
        unidade = salvarUnidade("Unidade do filtro");
        outraUnidade = salvarUnidade("Outra unidade");
        administrador = papel("ADMINISTRADOR");
        monitor = papel("MONITOR");
        totalAnterior = membros.filtrar(null, unidade.getId(), PageRequest.of(0, 1)).getTotalElements();
        globalSemVinculo = salvarMembro("Global sem vinculo", administrador);
        globalComVinculos = salvarMembro("Global com vinculos", administrador, unidade, outraUnidade);
        monitorLocal = salvarMembro("Monitor local", monitor, unidade);
        monitorExterno = salvarMembro("Monitor externo", monitor, outraUnidade);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void filtroIncluiAdministradoresGlobaisSemDuplicarMembrosOuTotais() {
        var pagina = membros.filtrar(null, unidade.getId(), PageRequest.of(0, (int) totalAnterior + 10));

        assertThat(pagina.getContent()).extracting(Membro::getId)
                .contains(globalSemVinculo.getId(), globalComVinculos.getId(), monitorLocal.getId())
                .doesNotContain(monitorExterno.getId()).doesNotHaveDuplicates();
        assertThat(pagina.getTotalElements()).isEqualTo(totalAnterior + 3);
    }

    @Test
    void buscaComUnidadeMantemTermoPaginacaoETotalSemDuplicarAdministrador() {
        String termo = "%" + prefixo.toLowerCase(java.util.Locale.ROOT) + "%";
        var primeira = membros.buscar(termo, null, null, unidade.getId(),
                PageRequest.of(0, 2, Sort.by("id")));
        var segunda = membros.buscar(termo, null, null, unidade.getId(),
                PageRequest.of(1, 2, Sort.by("id")));

        assertThat(primeira.getContent()).extracting(Membro::getId)
                .containsExactly(globalSemVinculo.getId(), globalComVinculos.getId());
        assertThat(segunda.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(segunda.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(membros.buscar("%" + prefixo.toLowerCase(java.util.Locale.ROOT) + " ausente%",
                null, null, unidade.getId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void buscaDeAdministradoresIncluiGlobalSemVinculoAoFiltrarOutraUnidade() {
        var pagina = membros.buscar("%" + prefixo.toLowerCase(java.util.Locale.ROOT) + "%", null,
                administrador.getId(), outraUnidade.getId(), PageRequest.of(0, 10, Sort.by("id")));

        assertThat(pagina.getContent()).extracting(Membro::getId)
                .containsExactly(globalSemVinculo.getId(), globalComVinculos.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filtroDePapelContinuaExcluindoAdministradoresAoSelecionarMonitor() {
        var pagina = membros.filtrar(monitor.getId(), unidade.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void consultaDoCoordenadorPermaneceRestritaAosMonitoresDaUnidade() {
        var pagina = membros.listarMonitoresDasUnidades(List.of(unidade.getId()), null,
                unidade.getId(), null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    private Papel papel(String nome) {
        var existentes = entityManager.createQuery("SELECT p FROM Papel p WHERE p.nomePapel = :nome", Papel.class)
                .setParameter("nome", nome).getResultList();
        if (!existentes.isEmpty()) return existentes.get(0);
        Papel papel = new Papel(null, nome, "");
        entityManager.persist(papel);
        return papel;
    }

    private Unidade salvarUnidade(String nome) {
        Unidade nova = new Unidade();
        nova.setNome(nome);
        nova.setEndereco("Rua de teste, 1");
        nova.setTelefone("11999999999");
        nova.setEmail(UUID.randomUUID() + "@example.com");
        nova.setDiasFuncionamento("Segunda a sexta");
        nova.setHorarioAbertura(LocalTime.of(8, 0));
        nova.setHorarioFechamento(LocalTime.of(18, 0));
        nova.setIdadeMin(6);
        nova.setIdadeMax(17);
        entityManager.persist(nova);
        return nova;
    }

    private Membro salvarMembro(String nome, Papel papel, Unidade... unidades) {
        Membro novo = new Membro(null, prefixo + " " + nome, UUID.randomUUID() + "@example.com",
                "senha-teste", Long.toString(ThreadLocalRandom.current().nextLong(10000000000L, 100000000000L)),
                null, null, papel);
        novo.setUnidades(List.of(unidades));
        entityManager.persist(novo);
        return novo;
    }
}
