package br.org.larescolaredencao.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.Turma;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.Parentesco;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.service.ContatoService;
import br.org.larescolaredencao.service.EventoService;
import br.org.larescolaredencao.service.UsuarioService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "LER_TEST_MYSQL", matches = "true")
class MembroRepositoryTest {
    @Autowired private MembroRepository membros;
    @Autowired private EntityManager entityManager;
    @Autowired private UsuarioService usuarios;
    @Autowired private ContatoService contatos;
    @Autowired private EventoService eventos;

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
        var pagina = membros.listarParaCoordenador(monitorExterno.getId(), List.of(unidade.getId()), null,
                unidade.getId(), null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void proprioPerfilEntraUmaVezNaPrimeiraPaginaQuandoAtendeAosFiltros() {
        var coordenador = salvarMembro("Coordenador", papel("COORDENADOR"), unidade, outraUnidade);
        String termo = "%" + prefixo.toLowerCase(java.util.Locale.ROOT) + "%";
        var paginacao = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("id")));

        var primeira = membros.listarParaCoordenador(coordenador.getId(), List.of(unidade.getId()), null,
                unidade.getId(), termo, null, paginacao);
        var segunda = membros.listarParaCoordenador(coordenador.getId(), List.of(unidade.getId()), null,
                unidade.getId(), termo, null, paginacao.next());

        assertThat(primeira.getContent()).extracting(Membro::getId).containsExactly(coordenador.getId());
        assertThat(segunda.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(primeira.getTotalElements()).isEqualTo(2);
        assertThat(segunda.getTotalElements()).isEqualTo(2);

        var filtrada = membros.listarParaCoordenador(coordenador.getId(), List.of(unidade.getId()), monitor.getId(),
                unidade.getId(), termo, null, paginacao);
        assertThat(filtrada.getContent()).extracting(Membro::getId).containsExactly(monitorLocal.getId());
        assertThat(filtrada.getTotalElements()).isEqualTo(1);
    }

    @Test
    void usuariosOrdenamAntesDePaginarSemRepetirMatriculas() {
        Turma turmaAlfa = salvarTurma(salvarUnidade("Alfa"), Periodo.MANHA);
        Turma turmaZulu = salvarTurma(salvarUnidade("Zulu"), Periodo.TARDE);
        Usuario antigo = salvarUsuario("Carlos", turmaAlfa);
        Usuario recente = salvarUsuario("Ana", turmaZulu);
        salvarMatricula(recente, turmaZulu);

        var pagina = PageRequest.of(0, 1);
        var primeira = usuarios.listarUsuariosPaginados(globalSemVinculo.getId(), prefixo, null, null, null, pagina);
        var segunda = usuarios.listarUsuariosPaginados(globalSemVinculo.getId(), prefixo, null, null, null, pagina.next());
        assertThat(primeira.getContent()).extracting(dto -> dto.getId()).containsExactly(recente.getId());
        assertThat(segunda.getContent()).extracting(dto -> dto.getId()).containsExactly(antigo.getId());
        assertThat(primeira.getTotalElements()).isEqualTo(2);
        assertThat(segunda.getTotalElements()).isEqualTo(2);

        var porTurma = usuarios.listarUsuariosPaginados(globalSemVinculo.getId(), prefixo, null, null, null,
                pagina.withSort(Sort.by("nomeTurma")));
        assertThat(porTurma.getContent()).extracting(dto -> dto.getId()).containsExactly(antigo.getId());
        var porNome = usuarios.listarUsuariosPaginados(globalSemVinculo.getId(), prefixo, null, null, null,
                pagina.withSort(Sort.by("nomeCompleto")));
        assertThat(porNome.getContent()).extracting(dto -> dto.getId()).containsExactly(recente.getId());
        var filtrada = usuarios.listarUsuariosPaginados(globalSemVinculo.getId(), prefixo, null,
                turmaZulu.getId(), null, pagina);
        assertThat(filtrada.getContent()).extracting(dto -> dto.getId()).containsExactly(recente.getId());
        assertThat(filtrada.getTotalElements()).isEqualTo(1);
    }

    @Test
    void contatosOrdenamPorVinculosNoBancoComDesempatePorId() {
        var usuario = salvarUsuario("Usuário", salvarTurma(unidade, Periodo.MANHA));
        var comVinculo = salvarContato("com vínculo");
        var semVinculo = salvarContato("sem vínculo");
        var maisNovo = salvarContato("sem vínculo mais novo");
        ContatoUsuario vinculo = new ContatoUsuario();
        vinculo.setContato(comVinculo);
        vinculo.setUsuario(usuario);
        vinculo.setParentesco(Parentesco.MAE);
        entityManager.persist(vinculo);

        var pagina = PageRequest.of(0, 1, Sort.by("quantidadeVinculos"));
        assertThat(contatos.listarContatos(globalSemVinculo.getId(), pagina, prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(maisNovo.getId());
        assertThat(contatos.listarContatos(globalSemVinculo.getId(), pagina.next(), prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(semVinculo.getId());
        var desc = contatos.listarContatos(globalSemVinculo.getId(), pagina.withSort(Sort.Direction.DESC,
                "quantidadeVinculos"), prefixo);
        assertThat(desc.getContent()).extracting(dto -> dto.getId()).containsExactly(comVinculo.getId());
        assertThat(desc.getTotalElements()).isEqualTo(3);
    }

    @Test
    void eventosUsamHorarioDoEventoEIdMesmoAposEdicao() {
        var dia = LocalDateTime.of(2026, 8, 20, 10, 30);
        var primeiro = salvarEvento("mais tarde", dia.plusHours(1));
        var empatado = salvarEvento("mesmo horário", dia.plusHours(1));
        var ultimo = salvarEvento("mais cedo", dia);
        var pagina = PageRequest.of(0, 1);
        assertThat(eventos.getAllEventos(pagina, null, prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(empatado.getId());
        assertThat(eventos.getAllEventos(pagina.next(), null, prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(primeiro.getId());
        assertThat(eventos.getAllEventos(pagina.withSort(Sort.by("dataEvento")), null, prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(ultimo.getId());

        ultimo.setDescricao("Descrição atualizada");
        entityManager.flush();
        assertThat(eventos.getAllEventos(pagina, null, prefixo).getContent())
                .extracting(dto -> dto.getId()).containsExactly(empatado.getId());

        ultimo.setDataEvento(dia.plusHours(2));
        entityManager.flush();
        var reposicionado = eventos.getAllEventos(pagina, null, prefixo);
        assertThat(reposicionado.getContent()).extracting(dto -> dto.getId()).containsExactly(ultimo.getId());
        assertThat(reposicionado.getTotalElements()).isEqualTo(3);
    }

    private Turma salvarTurma(Unidade unidade, Periodo periodo) {
        Turma turma = new Turma();
        turma.setUnidade(unidade);
        turma.setPeriodo(periodo);
        turma.setHoraInicio(LocalTime.of(8, 0));
        turma.setHoraFim(LocalTime.of(11, 0));
        entityManager.persist(turma);
        return turma;
    }

    private Usuario salvarUsuario(String nome, Turma turma) {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(prefixo + " " + nome);
        usuario.setDataNascimento(LocalDate.of(2015, 1, 1));
        entityManager.persist(usuario);
        salvarMatricula(usuario, turma);
        return usuario;
    }

    private void salvarMatricula(Usuario usuario, Turma turma) {
        Matricula matricula = new Matricula();
        matricula.setUsuario(usuario);
        matricula.setTurma(turma);
        entityManager.persist(matricula);
    }

    private Contato salvarContato(String nome) {
        Contato contato = new Contato();
        contato.setNomeCompleto(prefixo + " " + nome);
        contato.setTelefone("11999999999");
        entityManager.persist(contato);
        return contato;
    }

    private Evento salvarEvento(String nome, LocalDateTime data) {
        Evento evento = new Evento();
        evento.setTitulo(prefixo + " " + nome);
        evento.setDescricao("Evento de teste");
        evento.setDataEvento(data);
        evento.setEndereco("Endereço de teste");
        evento.setTipoEvento(TipoEvento.CULTURAL);
        entityManager.persist(evento);
        return evento;
    }

    private Papel papel(String nome) {
        var existentes = entityManager.createQuery("SELECT p FROM Papel p WHERE p.nomePapel = :nome", Papel.class)
                .setParameter("nome", nome).getResultList();
        if (!existentes.isEmpty()) return existentes.get(0);
        Papel papel = new Papel(null, nome, "");
        entityManager.persist(papel);
        return papel;
    }

    @Test
    void somenteAdministradoresNaoImpedemExcluirUnidade() {
        Unidade soAdministradores = salvarUnidade("Unidade só com administradores");
        salvarMembro("Administrador vinculado", administrador, soAdministradores);
        entityManager.flush();
        assertThat(membros.existsByUnidadesIdAndPapelNomePapelNot(soAdministradores.getId(), "ADMINISTRADOR")).isFalse();
    }

    @Test
    void monitoresECoordenadoresImpedemExcluirSuasUnidades() {
        assertThat(membros.existsByUnidadesIdAndPapelNomePapelNot(unidade.getId(), "ADMINISTRADOR")).isTrue();
        Unidade soCoordenador = salvarUnidade("Unidade só com coordenador");
        salvarMembro("Coordenador vinculado", papel("COORDENADOR"), soCoordenador);
        entityManager.flush();
        assertThat(membros.existsByUnidadesIdAndPapelNomePapelNot(soCoordenador.getId(), "ADMINISTRADOR")).isTrue();
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
