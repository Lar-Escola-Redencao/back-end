package br.org.larescolaredencao.service.relatorio;

import br.org.larescolaredencao.dto.UsuarioResponseDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import br.org.larescolaredencao.repository.*;
import br.org.larescolaredencao.service.FrequenciaAcompanhamentoService;
import br.org.larescolaredencao.service.FrequenciaAcompanhamentoServiceTestAcesso;
import br.org.larescolaredencao.service.UsuarioService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RelatorioAcompanhamentoTest {
    final UsuarioService usuarios = mock(UsuarioService.class);
    final MatriculaRepository matriculas = mock(MatriculaRepository.class);
    final FrequenciaRepository frequencias = mock(FrequenciaRepository.class);
    final OcorrenciaRepository ocorrencias = mock(OcorrenciaRepository.class);
    final EntrevistaSocialRepository entrevistas = mock(EntrevistaSocialRepository.class);
    final MembroRepository membros = mock(MembroRepository.class);
    final Clock relogio = Clock.fixed(Instant.parse("2026-11-10T15:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    final RelatorioAcompanhamentoService service = new RelatorioAcompanhamentoService(usuarios,
            selecao(),
            frequencias, ocorrencias, entrevistas, membros,
            new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()), relogio);

    FrequenciaAcompanhamentoService selecao() {
        var f = new FrequenciaAcompanhamentoServiceTestAcesso(usuarios, matriculas, frequencias, relogio);
        return f;
    }

    Membro membro(String perfil) {
        var m = new Membro(); var papel = new Papel(); papel.setNomePapel(perfil); m.setPapel(papel);
        m.setId(99); m.setNomeCompleto("Responsável Exportação");
        var unidade = new Unidade(); unidade.setId(3); m.setUnidades(List.of(unidade)); return m;
    }

    Matricula matricula(int id, Periodo periodo, LocalDate ingresso, LocalDate desligamento) {
        var unidade = new Unidade(); unidade.setId(3); unidade.setNome("Sede São José");
        var turma = new Turma(); turma.setId(2); turma.setUnidade(unidade); turma.setPeriodo(periodo);
        turma.setHoraInicio(LocalTime.of(7, 0)); turma.setHoraFim(LocalTime.NOON);
        var m = new Matricula(); m.setId(id); m.setTurma(turma); m.setStatus(StatusMatricula.ATIVO);
        m.setDataIngresso(ingresso.atStartOfDay()); m.setDataDesligamento(desligamento); return m;
    }

    Frequencia freq(Matricula m, LocalDate dia, boolean presente) {
        var f = new Frequencia(); f.setMatricula(m); f.setDataRegistro(dia); f.setPresente(presente); return f;
    }

    Ocorrencia ocorrencia(Matricula m, LocalDateTime data, String descricao) {
        var o = new Ocorrencia(); o.setId(1); o.setMatricula(m); o.setDataOcorrencia(data); o.setDescricao(descricao);
        o.setTipoOcorrencia(TipoOcorrencia.SAUDE);
        var autor = new Membro(); autor.setId(5); autor.setNomeCompleto("Autora Original"); o.setMembro(autor);
        return o;
    }

    EntrevistaSocial entrevista(LocalDateTime data, String relato) {
        var e = new EntrevistaSocial(); e.setIdUsuario(10); e.setIdMembro(5); e.setDataEntrevista(data); e.setRelato(relato);
        return e;
    }

    void cenario(Matricula... ms) {
        var resposta = mock(UsuarioResponseDTO.class);
        when(resposta.getNomeCompleto()).thenReturn("Maria da Silva");
        when(usuarios.buscarUsuarioPorId(eq(10), any())).thenReturn(resposta);
        when(matriculas.listarParaAcompanhamento(eq(10), anyBoolean(), anyList())).thenReturn(List.of(ms));
        var autor = new Membro(); autor.setId(5); autor.setNomeCompleto("Autora Original");
        when(membros.findAllById(any())).thenReturn(List.of(autor));
    }

    String texto(byte[] pdf) throws Exception {
        try (var reader = new PdfReader(pdf)) {
            var extrator = new PdfTextExtractor(reader); var sb = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) sb.append(extrator.getTextFromPage(i)).append('\n');
            return sb.toString();
        }
    }

    @Test void usaAnoAtualComoPadraoEIncluiUltimoDiaNasConsultas() {
        var m = matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null);
        cenario(m);
        var dados = service.montar(10, null, null, null, membro("COORDENADOR"));
        assertThat(dados.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(dados.fim()).isEqualTo(LocalDate.of(2026, 12, 31));
        verify(frequencias).findPorMatriculasNoPeriodo(List.of(7), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        verify(ocorrencias).listarParaRelatorioAcompanhamento(List.of(7),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        verify(entrevistas).listarParaRelatorio(10, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    @Test void filtroDeMatriculaRestringeFrequenciasEOcorrenciasMasEntrevistasSaoDoUsuario() {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null),
                matricula(8, Periodo.TARDE, LocalDate.of(2025, 1, 1), null));
        var dados = service.montar(10, 8, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro("ADMINISTRADOR"));
        verify(frequencias).findPorMatriculasNoPeriodo(List.of(8), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        verify(ocorrencias).listarParaRelatorioAcompanhamento(eq(List.of(8)), any(), any());
        verify(entrevistas).listarParaRelatorio(eq(10), any(), any());
        assertThat(dados.filtroMatricula()).contains("Matrícula 8", "Tarde");
    }

    @Test void matriculaForaDoAcessoEPeriodoInvalidoSaoRecusados() {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null));
        assertThatThrownBy(() -> service.montar(10, 99, null, null, membro("MONITOR")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThatThrownBy(() -> service.montar(10, null, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1), membro("MONITOR")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> service.montar(10, null, LocalDate.of(2026, 5, 2), null, membro("MONITOR")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test void monitorNuncaConsultaNemRecebeEntrevistas() throws Exception {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null));
        var arquivo = service.gerar(10, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro("MONITOR"));
        verifyNoInteractions(entrevistas);
        var texto = texto(arquivo.conteudo());
        assertThat(texto).doesNotContain("Entrevistas").doesNotContain("Nenhuma entrevista");
        assertThat(texto).contains("Ocorrências", "Nenhuma ocorrência registrada no período.");
    }

    @Test void coordenadorEAdminRecebemEntrevistasComMensagemQuandoVazias() throws Exception {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null));
        for (String perfil : List.of("COORDENADOR", "ADMINISTRADOR")) {
            var arquivo = service.gerar(10, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro(perfil));
            assertThat(texto(arquivo.conteudo())).contains("Entrevistas", "Nenhuma entrevista registrada no período.");
        }
    }

    @Test void nomeDoArquivoTemIdentificacaoEDataSemHorario() {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null));
        var arquivo = service.gerar(10, null, null, null, membro("ADMINISTRADOR"));
        assertThat(arquivo.nome()).isEqualTo("acompanhamento_maria-da-silva_2026-11-10.pdf");
        assertThat(arquivo.tipoConteudo()).isEqualTo("application/pdf");
    }

    @Test void calendarioMultiplasMatriculasIntervaloEntreAnosDescricoesLongasECabecalho() throws Exception {
        var m7 = matricula(7, Periodo.MANHA, LocalDate.of(2025, 11, 1), LocalDate.of(2025, 12, 20));
        var m8 = matricula(8, Periodo.TARDE, LocalDate.of(2026, 1, 15), null);
        cenario(m7, m8);
        when(frequencias.findPorMatriculasNoPeriodo(any(), any(), any())).thenReturn(List.of(
                freq(m7, LocalDate.of(2025, 12, 1), true), freq(m7, LocalDate.of(2026, 1, 16), false),
                freq(m8, LocalDate.of(2026, 1, 16), true), freq(m8, LocalDate.of(2026, 3, 31), true)));
        var longa = ("Linha de descrição longa. ".repeat(40) + "\n\nSegundo parágrafo preservado.\n").repeat(12);
        when(ocorrencias.listarParaRelatorioAcompanhamento(any(), any(), any())).thenReturn(List.of(
                ocorrencia(m8, LocalDateTime.of(2026, 3, 1, 9, 5), longa),
                ocorrencia(m7, LocalDateTime.of(2025, 12, 2, 8, 0), "Curta")));
        when(entrevistas.listarParaRelatorio(any(), any(), any())).thenReturn(List.of(
                entrevista(LocalDateTime.of(2026, 2, 3, 14, 30), "Relato da entrevista\n\nSegundo parágrafo da entrevista")));

        var dados = service.montar(10, null, LocalDate.of(2025, 11, 15), LocalDate.of(2026, 3, 31), membro("ADMINISTRADOR"));
        assertThat(dados.situacao(LocalDate.of(2025, 11, 14))).isEqualTo(DadosAcompanhamento.Situacao.FORA_DO_PERIODO);
        assertThat(dados.situacao(LocalDate.of(2025, 12, 5))).isEqualTo(DadosAcompanhamento.Situacao.SEM_REGISTRO);
        assertThat(dados.situacao(LocalDate.of(2026, 1, 5))).isEqualTo(DadosAcompanhamento.Situacao.SEM_MATRICULA)
                .as("sem registro nunca vira falta");
        assertThat(dados.registros().get(LocalDate.of(2026, 1, 16))).hasSize(2);

        byte[] pdf = new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados);
        var texto = texto(pdf);
        assertThat(texto).contains("Maria da Silva", "15/11/2025 a 31/03/2026", "Todas as matrículas",
                "10/11/2026 12:30", "Responsável Exportação", "2025", "2026", "Novembro de 2025", "Dezembro de 2025",
                "Janeiro de 2026", "Fevereiro de 2026", "Março de 2026", "M7 P", "M8 F".replace("M8 F", "M8 P"),
                "Segundo parágrafo preservado.", "Segundo parágrafo da entrevista", "Autora Original");
        assertThat(texto).doesNotContain("Abril de 2026").doesNotContain("Outubro de 2025");
        Path saida = Path.of(System.getProperty("acompanhamento.pdf.saida", "build/tmp/acompanhamento-exemplo.pdf"));
        Files.createDirectories(saida.getParent());
        Files.write(saida, pdf);
    }

    @Test void mesUnicoCentralizadoEOrdenacaoDecrescenteDasTabelas() throws Exception {
        var m = matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null);
        cenario(m);
        when(ocorrencias.listarParaRelatorioAcompanhamento(any(), any(), any())).thenReturn(List.of(
                ocorrencia(m, LocalDateTime.of(2026, 3, 20, 9, 5), "RecenteOc"),
                ocorrencia(m, LocalDateTime.of(2026, 3, 2, 9, 5), "AntigaOc")));
        var dados = service.montar(10, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro("MONITOR"));
        var texto = texto(new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados));
        assertThat(texto.indexOf("RecenteOc")).isLessThan(texto.indexOf("AntigaOc")).isPositive();
        assertThat(texto).contains("Março de 2026").doesNotContain("Fevereiro de 2026");
    }
}
