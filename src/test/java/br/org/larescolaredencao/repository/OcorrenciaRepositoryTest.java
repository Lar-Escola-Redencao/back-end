package br.org.larescolaredencao.repository;

import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "LER_TEST_MYSQL", matches = "true")
@TestPropertySource(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/ler_correcao_cep_senha")
class OcorrenciaRepositoryTest {
    @Autowired private OcorrenciaRepository ocorrencias;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private MembroRepository membros;

    private Ocorrencia salvar(LocalDateTime fato) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setMatricula(matriculas.getReferenceById(1));
        ocorrencia.setMembro(membros.getReferenceById(1));
        ocorrencia.setDataOcorrencia(fato);
        ocorrencia.setDataCriacao(LocalDateTime.now());
        ocorrencia.setDescricao("Validação transacional de datas");
        ocorrencia.setTipoOcorrencia(TipoOcorrencia.OUTRO);
        return ocorrencias.saveAndFlush(ocorrencia);
    }

    @Test
    void consultasPorDiaDevemIncluirTodosOsHorariosSemIncluirDiaSeguinte() {
        LocalDate dia = LocalDate.of(2099, 1, 15);
        Ocorrencia inicio = salvar(dia.atStartOfDay());
        Ocorrencia fim = salvar(dia.atTime(23, 59, 59));
        Ocorrencia seguinte = salvar(dia.plusDays(1).atStartOfDay());
        Integer turmaId = inicio.getMatricula().getTurma().getId();
        assertThat(ocorrencias.findByMatriculaIdAndDataOcorrencia(1, dia))
                .extracting(Ocorrencia::getId).contains(inicio.getId(), fim.getId()).doesNotContain(seguinte.getId());
        assertThat(ocorrencias.findByMatriculaTurmaIdAndDataOcorrencia(turmaId, dia))
                .extracting(Ocorrencia::getId).contains(inicio.getId(), fim.getId()).doesNotContain(seguinte.getId());
    }

    @Test
    void limpezaDeMatriculaDeveRespeitarDiaInclusivoEExclusivo() {
        LocalDate dia = LocalDate.of(2099, 1, 15);
        Ocorrencia anterior = salvar(dia.minusDays(1).atTime(23, 59, 59));
        Ocorrencia mesmoDia = salvar(dia.atTime(23, 59, 59));
        Ocorrencia seguinte = salvar(dia.plusDays(1).atStartOfDay());
        ocorrencias.deleteByMatriculaIdAndDataOcorrenciaAfter(1, dia);
        assertThat(ocorrencias.findByMatriculaIdAndDataOcorrencia(1, dia.plusDays(1))).isEmpty();
        assertThat(ocorrencias.findByMatriculaIdAndDataOcorrencia(1, dia))
                .extracting(Ocorrencia::getId).contains(mesmoDia.getId());
        ocorrencias.deleteByMatriculaIdAndDataOcorrenciaGreaterThanEqual(1, dia);
        assertThat(ocorrencias.findByMatriculaIdAndDataOcorrencia(1, dia)).isEmpty();
        assertThat(ocorrencias.findByMatriculaIdAndDataOcorrencia(1, dia.minusDays(1)))
                .extracting(Ocorrencia::getId).contains(anterior.getId());
    }
}
