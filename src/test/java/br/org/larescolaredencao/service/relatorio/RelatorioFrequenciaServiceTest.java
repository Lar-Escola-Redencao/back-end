package br.org.larescolaredencao.service.relatorio;

import br.org.larescolaredencao.dto.RelatorioPresencasDTO;
import br.org.larescolaredencao.dto.RelatorioResumoDTO;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.FormatoRelatorio;
import br.org.larescolaredencao.model.enums.Periodo;
import br.org.larescolaredencao.model.enums.StatusMatricula;
import br.org.larescolaredencao.model.enums.TipoOcorrencia;
import br.org.larescolaredencao.repository.*;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioFrequenciaServiceTest {
    @Mock private TurmaRepository turmas;
    @Mock private MatriculaRepository matriculas;
    @Mock private FrequenciaRepository frequencias;
    @Mock private OcorrenciaRepository ocorrencias;
    private RelatorioFrequenciaService service;
    private Turma turma;
    private Unidade unidade;
    private Membro coordenador;
    private final List<Matricula> matriculasTurma = new ArrayList<>();
    private final List<Frequencia> frequenciasTurma = new ArrayList<>();
    private final List<Ocorrencia> ocorrenciasTurma = new ArrayList<>();
    private int sequencia = 1;

    @BeforeEach
    void preparar() {
        service = new RelatorioFrequenciaService(turmas, matriculas, frequencias, ocorrencias,
                new RelatorioPdfGerador(), new RelatorioXlsxGerador());
        unidade = new Unidade();
        unidade.setId(1);
        unidade.setNome("Unidade Centro");
        turma = new Turma();
        turma.setId(1);
        turma.setUnidade(unidade);
        turma.setPeriodo(Periodo.MANHA);
        turma.setHoraInicio(LocalTime.of(7, 0));
        turma.setHoraFim(LocalTime.of(11, 0));
        coordenador = membro("COORDENADOR", unidade);
        lenient().when(turmas.findById(1)).thenReturn(Optional.of(turma));
        lenient().when(matriculas.findVigentesPorTurmaNoPeriodo(eq(1), any(), any())).thenAnswer(inv -> {
            LocalDateTime fimExclusivo = inv.getArgument(1);
            LocalDate inicio = inv.getArgument(2);
            return matriculasTurma.stream()
                    .filter(m -> m.getDataIngresso().isBefore(fimExclusivo)
                            && (m.getDataDesligamento() == null || !m.getDataDesligamento().isBefore(inicio)))
                    .toList();
        });
        lenient().when(frequencias.findPorTurmaNoPeriodo(eq(1), any(), any())).thenAnswer(inv -> {
            LocalDate inicio = inv.getArgument(1);
            LocalDate fim = inv.getArgument(2);
            return frequenciasTurma.stream()
                    .filter(f -> !f.getDataRegistro().isBefore(inicio) && !f.getDataRegistro().isAfter(fim))
                    .toList();
        });
        lenient().when(ocorrencias.findPorTurmaNoPeriodo(eq(1), any(), any())).thenAnswer(inv -> {
            LocalDateTime inicio = inv.getArgument(1);
            LocalDateTime fim = inv.getArgument(2);
            return ocorrenciasTurma.stream()
                    .filter(o -> !o.getDataOcorrencia().isBefore(inicio) && o.getDataOcorrencia().isBefore(fim))
                    .toList();
        });
        lenient().when(matriculas.findIdsUsuariosExcluidos(anyCollection())).thenAnswer(inv -> matriculasTurma.stream()
                .filter(m -> m.getStatus() == StatusMatricula.EXCLUIDO)
                .map(m -> m.getUsuario().getId())
                .toList());
    }

    private Membro membro(String papel, Unidade... unidades) {
        Papel p = new Papel();
        p.setNomePapel(papel);
        Membro membro = new Membro();
        membro.setId(sequencia++);
        membro.setNomeCompleto("Membro " + papel);
        membro.setPapel(p);
        membro.setUnidades(List.of(unidades));
        return membro;
    }

    private Matricula matricula(String nome, LocalDate ingresso, LocalDate desligamento, StatusMatricula status) {
        Usuario usuario = new Usuario();
        usuario.setId(sequencia++);
        usuario.setNomeCompleto(nome);
        Matricula matricula = new Matricula();
        matricula.setId(sequencia++);
        matricula.setUsuario(usuario);
        matricula.setTurma(turma);
        matricula.setDataIngresso(ingresso.atTime(9, 0));
        matricula.setDataDesligamento(desligamento);
        matricula.setStatus(status);
        usuario.setMatriculas(List.of(matricula));
        matriculasTurma.add(matricula);
        return matricula;
    }

    private void frequencia(Matricula matricula, LocalDate data, boolean presente) {
        Frequencia frequencia = new Frequencia();
        frequencia.setId(sequencia++);
        frequencia.setMatricula(matricula);
        frequencia.setDataRegistro(data);
        frequencia.setPresente(presente);
        frequenciasTurma.add(frequencia);
    }

    private Ocorrencia ocorrencia(Matricula matricula, LocalDateTime data, String descricao, Membro autor) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setId(sequencia++);
        ocorrencia.setMatricula(matricula);
        ocorrencia.setDataOcorrencia(data);
        ocorrencia.setDataCriacao(data.plusDays(5));
        ocorrencia.setDescricao(descricao);
        ocorrencia.setTipoOcorrencia(TipoOcorrencia.COMPORTAMENTO);
        ocorrencia.setMembro(autor);
        ocorrenciasTurma.add(ocorrencia);
        return ocorrencia;
    }

    private RelatorioPresencasDTO presencas(LocalDate inicio, LocalDate fim, FormatoRelatorio formato) {
        RelatorioPresencasDTO dto = new RelatorioPresencasDTO();
        dto.setIdTurma(1);
        dto.setDataInicio(inicio);
        dto.setDataFim(fim);
        dto.setFormato(formato);
        return dto;
    }

    private RelatorioResumoDTO resumo(LocalDate inicio, LocalDate fim, FormatoRelatorio formato) {
        RelatorioResumoDTO dto = new RelatorioResumoDTO();
        dto.setIdsTurmas(List.of(1));
        dto.setDataInicio(inicio);
        dto.setDataFim(fim);
        dto.setFormato(formato);
        return dto;
    }

    @Test
    void indicadoresNaoTransformamAusenciaDeRegistroEmFaltaOuPresenca() {
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        Matricula ana = matricula("Ana", LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 10), StatusMatricula.INATIVO);
        frequencia(ana, LocalDate.of(2026, 3, 3), true);
        frequencia(ana, LocalDate.of(2026, 3, 4), false);
        frequencia(ana, LocalDate.of(2026, 3, 12), true);

        DadosRelatorio.Presencas dados = service.montarPresencas(turma, inicio, LocalDate.of(2026, 3, 31), true,
                LocalDateTime.of(2026, 4, 1, 10, 0));

        var indicadores = dados.alunos().get(0).indicadores();
        assertThat(indicadores.get(LocalDate.of(2026, 3, 2))).isEqualTo(Indicador.NAO_MATRICULADO);
        assertThat(indicadores.get(LocalDate.of(2026, 3, 3))).isEqualTo(Indicador.PRESENTE);
        assertThat(indicadores.get(LocalDate.of(2026, 3, 4))).isEqualTo(Indicador.FALTA);
        assertThat(indicadores.get(LocalDate.of(2026, 3, 5))).isEqualTo(Indicador.NAO_REGISTRADO);
        assertThat(indicadores.get(LocalDate.of(2026, 3, 11))).isEqualTo(Indicador.NAO_MATRICULADO);
        assertThat(indicadores.get(LocalDate.of(2026, 3, 12))).isEqualTo(Indicador.PRESENTE_FORA_DA_MATRICULA);
        assertThat(dados.possuiRegistroForaDaMatricula()).isTrue();
    }

    @Test
    void resumoCalculaPercentualSomenteSobreDiasRegistrados() {
        Matricula ana = matricula("Ana", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        matricula("Bruno", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        for (int dia = 1; dia <= 15; dia++) {
            frequencia(ana, LocalDate.of(2026, 3, dia), dia > 3);
        }

        var dados = service.montarResumo(turma, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        var linhaAna = dados.alunos().get(0);
        assertThat(linhaAna.presencas()).isEqualTo(12);
        assertThat(linhaAna.faltas()).isEqualTo(3);
        assertThat(linhaAna.diasRegistrados()).isEqualTo(15);
        assertThat(linhaAna.percentualArredondado()).isEqualByComparingTo("80.0");
        var linhaBruno = dados.alunos().get(1);
        assertThat(linhaBruno.percentual()).isNull();
        assertThat(linhaBruno.observacao()).isEqualTo(DadosRelatorio.SEM_FREQUENCIA);
    }

    @Test
    void alunosOrdenadosAlfabeticamenteEExcluidosSomenteComHistorico() {
        matricula("Érica", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        matricula("Bruno", LocalDate.of(2026, 1, 1), null, StatusMatricula.EGRESSO).setDataDesligamento(LocalDate.of(2026, 3, 5));
        matricula("Carla", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 20), StatusMatricula.EXCLUIDO);
        Matricula davi = matricula("Davi", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 20), StatusMatricula.EXCLUIDO);
        frequencia(davi, LocalDate.of(2026, 3, 2), true);
        matricula("Abel", LocalDate.of(2026, 4, 1), null, StatusMatricula.ATIVO);

        var dados = service.montarPresencas(turma, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), false,
                LocalDateTime.now());

        assertThat(dados.alunos()).extracting(a -> a.aluno().nomeExibicao())
                .containsExactly("Bruno", "Davi (excluído)", "Érica");
        assertThat(dados.ocorrencias()).isNull();
    }

    @Test
    void ocorrenciasFiltradasPelaDataDoFatoEOrdenadasComAutorOriginal() {
        Membro autor = membro("MONITOR", unidade);
        Matricula bia = matricula("Bia", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        Matricula ana = matricula("Ana", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        ocorrencia(bia, LocalDateTime.of(2026, 3, 10, 8, 0), "Segunda", autor);
        ocorrencia(ana, LocalDateTime.of(2026, 3, 10, 8, 0), "Primeira", autor);
        ocorrencia(ana, LocalDateTime.of(2026, 2, 28, 10, 0), "Fora do período", autor)
                .setDataCriacao(LocalDateTime.of(2026, 3, 2, 10, 0));

        var dados = service.montarPresencas(turma, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), true,
                LocalDateTime.now());

        assertThat(dados.ocorrencias()).extracting(DadosRelatorio.Ocorrencia::descricao)
                .containsExactly("Primeira", "Segunda");
        assertThat(dados.ocorrencias()).extracting(DadosRelatorio.Ocorrencia::autor).containsOnly("Membro MONITOR");
    }

    @Test
    void blocosRespeitamMesesDe28A31Dias() {
        assertThat(RelatorioPdfGerador.blocos(dias(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))))
                .extracting(RelatorioPdfGerador.BlocoDatas::rotulo)
                .containsExactly("01 – 16 de fevereiro de 2026", "17 – 28 de fevereiro de 2026");
        assertThat(RelatorioPdfGerador.blocos(dias(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29))))
                .extracting(b -> b.datas().size()).containsExactly(16, 13);
        assertThat(RelatorioPdfGerador.blocos(dias(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))))
                .extracting(b -> b.datas().size()).containsExactly(16, 14);
        assertThat(RelatorioPdfGerador.blocos(dias(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 17))))
                .extracting(RelatorioPdfGerador.BlocoDatas::rotulo)
                .containsExactly("10 – 16 de março de 2026", "17 – 31 de março de 2026",
                        "01 – 16 de abril de 2026", "17 de abril de 2026");
    }

    private List<LocalDate> dias(LocalDate inicio, LocalDate fim) {
        return inicio.datesUntil(fim.plusDays(1)).toList();
    }

    @Test
    void pdfDetalhadoApresentaTodosOsAlunosDoBlocoAntesDoProximoERepeteCabecalho() throws Exception {
        for (int i = 1; i <= 45; i++) {
            matricula(String.format("Aluno %02d", i), LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        }
        RelatorioPresencasDTO dto = presencas(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.PDF);

        ArquivoRelatorio arquivo = service.gerarPresencas(dto, coordenador);

        assertThat(arquivo.nome()).isEqualTo("presencas-detalhadas_unidade-centro_manha-07h00_2026-03-01_a_2026-03-31.pdf");
        PdfReader reader = new PdfReader(arquivo.conteudo());
        PdfTextExtractor extrator = new PdfTextExtractor(reader);
        int paginas = reader.getNumberOfPages();
        assertThat(paginas).isGreaterThanOrEqualTo(5);
        List<String> textos = new ArrayList<>();
        for (int p = 1; p <= paginas; p++) textos.add(extrator.getTextFromPage(p));

        int ultimaPaginaPrimeiroBloco = -1;
        for (int p = 0; p < paginas; p++) {
            if (textos.get(p).contains("01 – 16 de março de 2026")) {
                assertThat(textos.get(p)).contains("Aluno").contains("Unidade Centro");
                ultimaPaginaPrimeiroBloco = p;
            }
        }
        assertThat(ultimaPaginaPrimeiroBloco).isGreaterThanOrEqualTo(1);
        assertThat(textos.get(ultimaPaginaPrimeiroBloco)).contains("Aluno 45");
        assertThat(textos.get(ultimaPaginaPrimeiroBloco + 1)).contains("17 – 31 de março de 2026").contains("Aluno 01");
        assertThat(textos.get(paginas - 1)).contains(DadosRelatorio.SEM_OCORRENCIAS);
    }

    @Test
    void xlsxDetalhadoTemAbaPorMesTextoSeguroEPaineisCongelados() throws Exception {
        Matricula ana = matricula("=HYPERLINK(\"x\")", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        frequencia(ana, LocalDate.of(2026, 3, 31), true);
        ocorrencia(ana, LocalDateTime.of(2026, 4, 2, 8, 30), "+descrição longa", membro("MONITOR", unidade));
        RelatorioPresencasDTO dto = presencas(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 2), FormatoRelatorio.XLSX);

        ArquivoRelatorio arquivo = service.gerarPresencas(dto, coordenador);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
            assertThat(workbook.getSheetName(1)).isEqualTo("2026-03");
            assertThat(workbook.getSheetName(2)).isEqualTo("2026-04");
            assertThat(workbook.getSheetName(3)).isEqualTo("Ocorrências");
            Sheet marco = workbook.getSheet("2026-03");
            assertThat(marco.getRow(0).getLastCellNum()).isEqualTo((short) 3);
            assertThat(marco.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(marco.getRow(1).getCell(0).getCellStyle().getQuotePrefixed()).isTrue();
            assertThat(marco.getRow(1).getCell(1).getStringCellValue()).isEqualTo("—");
            assertThat(marco.getRow(1).getCell(2).getStringCellValue()).isEqualTo("P");
            assertThat(marco.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
            assertThat(marco.getPaneInformation().getVerticalSplitPosition()).isEqualTo((short) 1);
            Sheet ocorrenciasAba = workbook.getSheet("Ocorrências");
            assertThat(ocorrenciasAba.getRow(1).getCell(3).getStringCellValue()).isEqualTo("+descrição longa");
        }
    }

    @Test
    void xlsxResumoGravaNumerosEPercentualVazioSemRegistro() throws Exception {
        Matricula ana = matricula("Ana", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        matricula("Bruno", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        frequencia(ana, LocalDate.of(2026, 3, 2), true);
        frequencia(ana, LocalDate.of(2026, 3, 3), true);
        frequencia(ana, LocalDate.of(2026, 3, 4), false);

        ArquivoRelatorio arquivo = service.gerarResumo(resumo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                FormatoRelatorio.XLSX), coordenador);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo.conteudo()))) {
            Sheet aba = workbook.getSheetAt(1);
            assertThat(aba.getSheetName()).isEqualTo("Unidade Centro - Manhã 07h00");
            assertThat(aba.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(2);
            assertThat(aba.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(3);
            assertThat(aba.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(0.667);
            assertThat(aba.getRow(1).getCell(4).getCellStyle().getDataFormatString()).isEqualTo("0.0%");
            assertThat(aba.getRow(2).getCell(4).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(aba.getRow(2).getCell(5).getStringCellValue()).isEqualTo(DadosRelatorio.SEM_FREQUENCIA);
            assertThat(aba.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        }
    }

    @Test
    void pdfResumoGeraUmaPaginaPorTurma() throws Exception {
        Turma tarde = new Turma();
        tarde.setId(2);
        tarde.setUnidade(unidade);
        tarde.setPeriodo(Periodo.TARDE);
        tarde.setHoraInicio(LocalTime.of(13, 0));
        tarde.setHoraFim(LocalTime.of(17, 0));
        when(turmas.findById(2)).thenReturn(Optional.of(tarde));
        Matricula ana = matricula("Ana", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        frequencia(ana, LocalDate.of(2026, 3, 2), true);
        RelatorioResumoDTO dto = resumo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.PDF);
        dto.setIdsTurmas(List.of(2, 1));

        ArquivoRelatorio arquivo = service.gerarResumo(dto, coordenador);

        PdfReader reader = new PdfReader(arquivo.conteudo());
        PdfTextExtractor extrator = new PdfTextExtractor(reader);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(extrator.getTextFromPage(1)).contains("Manhã").contains("100,0%").contains("Percentual calculado");
        assertThat(extrator.getTextFromPage(2)).contains("Tarde").contains(DadosRelatorio.SEM_ALUNOS);
        assertThat(arquivo.nome()).isEqualTo("resumo-frequencia_unidade-centro_2-turmas_2026-03-01_a_2026-03-31.pdf");
    }

    @Test
    void recusaExportacaoDeTurmaDeOutraUnidade() {
        Unidade outra = new Unidade();
        outra.setId(9);
        Membro monitor = membro("MONITOR", outra);

        assertThatThrownBy(() -> service.gerarPresencas(
                presencas(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.PDF), monitor))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.gerarResumo(
                resumo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.XLSX), monitor))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(frequencias, ocorrencias);
    }

    @Test
    void validaPeriodoELimites() {
        assertThatThrownBy(() -> service.gerarPresencas(
                presencas(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), FormatoRelatorio.PDF), coordenador))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.gerarPresencas(
                presencas(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 3, 1), FormatoRelatorio.PDF), coordenador))
                .hasMessageContaining("366 dias");
        RelatorioResumoDTO dto = resumo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.PDF);
        dto.setIdsTurmas(java.util.stream.IntStream.rangeClosed(1, 31).boxed().toList());
        assertThatThrownBy(() -> service.gerarResumo(dto, coordenador)).hasMessageContaining("30 turmas");
    }

    @Test
    void exportacaoNaoAlteraRegistros() {
        Matricula ana = matricula("Ana", LocalDate.of(2026, 1, 1), null, StatusMatricula.ATIVO);
        frequencia(ana, LocalDate.of(2026, 3, 2), true);

        service.gerarPresencas(presencas(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), FormatoRelatorio.XLSX),
                coordenador);

        verify(frequencias, never()).save(any());
        verify(frequencias, never()).saveAll(any());
        verify(matriculas, never()).save(any());
        verify(ocorrencias, never()).save(any());
    }

    @Test
    void nomesDeAbasSaoUnicosEValidos() {
        Set<String> usados = new HashSet<>();
        String primeiro = RelatorioXlsxGerador.nomeAbaUnico("Unidade [Centro]: Manhã (07:00 – 11:00)", usados);
        String segundo = RelatorioXlsxGerador.nomeAbaUnico("Unidade [Centro]: Manhã (07:00 – 11:00)", usados);
        assertThat(primeiro).hasSizeLessThanOrEqualTo(31).doesNotContain("[", "]", ":");
        assertThat(segundo).hasSizeLessThanOrEqualTo(31).endsWith("(2)").isNotEqualTo(primeiro);
    }
}
