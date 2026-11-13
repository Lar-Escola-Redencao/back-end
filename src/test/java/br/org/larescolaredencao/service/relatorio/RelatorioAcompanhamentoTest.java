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
            new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()), new RelatorioXlsxGerador(), relogio);

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

    @Test void filtroDeMatriculaRestringeFrequenciasEOcorrenciasMasEntrevistasSaoDoUsuario() throws Exception {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null),
                matricula(8, Periodo.TARDE, LocalDate.of(2025, 1, 1), null));
        var dados = service.montar(10, 8, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro("ADMINISTRADOR"));
        verify(frequencias).findPorMatriculasNoPeriodo(List.of(8), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        verify(ocorrencias).listarParaRelatorioAcompanhamento(eq(List.of(8)), any(), any());
        verify(entrevistas).listarParaRelatorio(eq(10), any(), any());
        assertThat(dados.filtroMatricula()).contains("Matrícula 8", "Tarde");
        assertThat(texto(new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados)))
                .contains("Matrículas", "Sede São José - Tarde (01/01/2025 a hoje)")
                .doesNotContain("Matrícula:", "Matrícula 8");
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(new RelatorioXlsxGerador().acompanhamento(dados)))) {
            for (var linha : workbook.getSheet("Informações")) {
                for (var celula : linha) {
                    assertThat(celula.getStringCellValue()).doesNotContain("Matrícula 8");
                }
                assertThat(linha.getCell(0).getStringCellValue()).isNotEqualTo("Matrícula");
            }
        }
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
        assertThat(texto).contains("Dados de acompanhamento individual", "Maria da Silva", "15/11/2025 a 31/03/2026",
                "10/11/2026 12:30", "Responsável Exportação", "2025", "2026", "Novembro de 2025", "Dezembro de 2025",
                "Janeiro de 2026", "Fevereiro de 2026", "Março de 2026", "Sede São José - Manhã (01/11/2025 a 20/12/2025)",
                "Sede São José - Tarde (15/01/2026 a hoje)",
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
    @Test void geraXlsxComLinhasDeFrequenciaOcorrenciasEEntrevistasSomenteParaPerfisPermitidos() throws Exception {
        var m7 = matricula(7, Periodo.MANHA, LocalDate.of(2025, 1, 1), null);
        var m8 = matricula(8, Periodo.TARDE, LocalDate.of(2025, 1, 1), null);
        cenario(m7, m8);
        when(frequencias.findPorMatriculasNoPeriodo(any(), any(), any())).thenReturn(List.of(
                freq(m7, LocalDate.of(2026, 3, 2), true), freq(m8, LocalDate.of(2026, 3, 2), false)));
        when(ocorrencias.listarParaRelatorioAcompanhamento(any(), any(), any())).thenReturn(List.of(
                ocorrencia(m7, LocalDateTime.of(2026, 3, 2, 9, 30), "Relato\n\nOutro parágrafo")));
        when(entrevistas.listarParaRelatorio(any(), any(), any())).thenReturn(List.of(
                entrevista(LocalDateTime.of(2026, 3, 3, 10, 0), "Entrevista reservada")));
        for (String perfil : List.of("ADMINISTRADOR", "COORDENADOR", "MONITOR")) {
            var arquivo = service.gerar(10, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), membro(perfil),
                    br.org.larescolaredencao.model.enums.FormatoRelatorio.XLSX);
            assertThat(arquivo.nome()).endsWith("2026-11-10.xlsx");
            assertThat(arquivo.tipoConteudo()).contains("spreadsheetml");
            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(arquivo.conteudo()))) {
                assertThat(workbook.getSheet("Informações").getRow(2).getCell(1).getStringCellValue())
                        .isEqualTo("Dados de acompanhamento individual");
                assertThat(workbook.getSheet("Informações").getColumnWidth(0)).isEqualTo(60 * 256);
                assertThat(workbook.getSheet("Sede São José - Manhã")).isNotNull();
                assertThat(workbook.getSheet("Sede São José - Tarde")).isNotNull();
                assertThat(workbook.getSheet("Ocorrências").getRow(1).getCell(2).getStringCellValue())
                        .isEqualTo("Relato\n\nOutro parágrafo");
                var frequencia = workbook.getSheet("Sede São José - Manhã");
                assertThat(frequencia.getRow(0).getLastCellNum()).isEqualTo((short) 2);
                assertThat(frequencia.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Data");
                assertThat(frequencia.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Situação do dia");
                assertThat(frequencia.getLastRowNum()).isEqualTo(31);
                assertThat(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(frequencia.getRow(2).getCell(0))).isTrue();
                assertThat(frequencia.getRow(2).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 2));
                assertThat(frequencia.getRow(2).getCell(1).getStringCellValue()).isEqualTo("P");
                assertThat(workbook.getSheet("Sede São José - Tarde").getRow(2).getCell(1).getStringCellValue()).isEqualTo("F");
                assertThat(workbook.getSheet("Entrevistas") != null).isEqualTo(!perfil.equals("MONITOR"));
            }
        }
    }

    @Test void distinguePresencaFaltaSemRegistroESemMatriculaNosDoisFormatos() throws Exception {
        var inicio = LocalDate.of(2026, 1, 1);
        var dados = new DadosAcompanhamento("Usuário de teste", inicio, LocalDate.of(2026, 3, 31), "Todas as matrículas",
                LocalDateTime.of(2026, 4, 1, 10, 0), "Responsável", List.of(
                        new DadosAcompanhamento.MatriculaRelatorio(7, "Unidade A", "Manhã", inicio, LocalDate.of(2026, 1, 15)),
                        new DadosAcompanhamento.MatriculaRelatorio(8, "Unidade B", "Tarde", LocalDate.of(2026, 3, 1), null)),
                Map.of(inicio.plusDays(1), List.of(new DadosAcompanhamento.RegistroDia(7, true)),
                        inicio.plusDays(2), List.of(new DadosAcompanhamento.RegistroDia(7, false))), List.of(), null);
        byte[] pdf = new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados);
        var conteudo = texto(pdf);
        assertThat(conteudo.indexOf("Matrículas")).isLessThan(conteudo.indexOf("Frequência"));
        assertThat(texto(pdf)).contains("Legenda:", "N/A = não matriculado na data", "frequência não registrada", "Unidade A - Manhã", "Unidade B - Tarde")
                .doesNotContain("M7", "M8", "cada registro do dia indica sua matrícula");
        byte[] xlsx = new RelatorioXlsxGerador().acompanhamento(dados);
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            var aba = workbook.getSheet("Unidade A - Manhã");
            assertThat(aba.getLastRowNum()).isEqualTo(15);
            assertThat(aba.getRow(1).getCell(1).getStringCellValue()).isEqualTo("—");
            assertThat(aba.getRow(2).getCell(1).getStringCellValue()).isEqualTo("P");
            assertThat(aba.getRow(3).getCell(1).getStringCellValue()).isEqualTo("F");
            assertThat(aba.getRow(1).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(inicio);
            assertThat(aba.getRow(15).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 15));
            var outraAba = workbook.getSheet("Unidade B - Tarde");
            assertThat(outraAba.getLastRowNum()).isEqualTo(31);
            assertThat(outraAba.getRow(1).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(outraAba.getRow(31).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(outraAba.getRow(1).getCell(1).getStringCellValue()).isEqualTo("—");
            for (var linha : aba) {
                assertThat(linha.getCell(1).getStringCellValue()).isNotEqualTo("N/A");
            }
        }
        Files.createDirectories(Path.of("build/tmp"));
        Files.write(Path.of("build/tmp/acompanhamento-layout.pdf"), pdf);
        Files.write(Path.of("build/tmp/acompanhamento-linhas.xlsx"), xlsx);
    }

    @Test void pdfSeparaSecoesEmPaginasEAgrupaOcorrenciasPelaMatriculaComDataEHoraJuntas() throws Exception {
        var m7 = matricula(7, Periodo.MANHA, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        var m8 = matricula(8, Periodo.TARDE, LocalDate.of(2026, 2, 1), null);
        cenario(m7, m8);
        when(ocorrencias.listarParaRelatorioAcompanhamento(any(), any(), any())).thenReturn(List.of(
                ocorrencia(m8, LocalDateTime.of(2026, 2, 2, 10, 5), "OcorrenciaTarde"),
                ocorrencia(m7, LocalDateTime.of(2026, 1, 15, 9, 30), "OcorrenciaManha")));
        when(entrevistas.listarParaRelatorio(any(), any(), any())).thenReturn(List.of(
                entrevista(LocalDateTime.of(2026, 2, 3, 14, 30), "EntrevistaSeparada")));
        byte[] pdf = service.gerar(10, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28), membro("ADMINISTRADOR")).conteudo();
        try (var reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(3);
            var extrator = new PdfTextExtractor(reader);
            assertThat(extrator.getTextFromPage(1)).contains("Frequência").doesNotContain("OcorrenciaManha", "EntrevistaSeparada");
            assertThat(extrator.getTextFromPage(2)).contains("Ocorrências", "Data e hora", "Tipo", "Descrição", "Autor",
                    "Sede São José - Manhã (01/01/2026 a 31/01/2026)",
                    "Sede São José - Tarde (01/02/2026 a hoje)",
                    "15/01/2026 09:30", "02/02/2026 10:05", "OcorrenciaManha", "OcorrenciaTarde")
                    .doesNotContain("Unidade e turma", "EntrevistaSeparada");
            assertThat(extrator.getTextFromPage(3)).contains("Entrevistas", "Data e hora", "03/02/2026 14:30", "EntrevistaSeparada")
                    .doesNotContain("OcorrenciaManha", "OcorrenciaTarde");
        }
        Files.write(Path.of("build/tmp/acompanhamento-secoes.pdf"), pdf);
    }

    @Test void isolaFrequenciasSimultaneasENomesDeAbasRepetidos() throws Exception {
        var dia = LocalDate.of(2026, 3, 2);
        var primeira = new DadosAcompanhamento.MatriculaRelatorio(7, "Unidade com nome muito longo para a aba", "Manhã", dia, null);
        var segunda = new DadosAcompanhamento.MatriculaRelatorio(8, primeira.unidade(), "Manhã", dia, null);
        var dados = new DadosAcompanhamento("Usuário", dia, dia.plusDays(1), "Todas as matrículas",
                dia.atTime(12, 0), "Responsável", List.of(primeira, segunda),
                Map.of(dia, List.of(new DadosAcompanhamento.RegistroDia(7, true), new DadosAcompanhamento.RegistroDia(8, false))),
                List.of(), null);
        assertThat(dados.paraMatricula(primeira).registros().get(dia))
                .containsExactly(new DadosAcompanhamento.RegistroDia(7, true));
        assertThat(dados.paraMatricula(segunda).registros().get(dia))
                .containsExactly(new DadosAcompanhamento.RegistroDia(8, false));
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(new RelatorioXlsxGerador().acompanhamento(dados)))) {
            var aba1 = workbook.getSheetAt(1);
            var aba2 = workbook.getSheetAt(2);
            assertThat(aba1.getSheetName()).hasSizeLessThanOrEqualTo(31);
            assertThat(aba2.getSheetName()).hasSizeLessThanOrEqualTo(31).isNotEqualTo(aba1.getSheetName());
            assertThat(aba1.getRow(1).getCell(1).getStringCellValue()).isEqualTo("P");
            assertThat(aba2.getRow(1).getCell(1).getStringCellValue()).isEqualTo("F");
        }
        var pdf = new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados);
        try (var reader = new PdfReader(pdf)) {
            var extrator = new PdfTextExtractor(reader);
            assertThat(extrator.getTextFromPage(1)).contains(primeira.unidade()).doesNotContain("M7", "M8");
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
            assertThat(extrator.getTextFromPage(1)).contains("Matrículas", "Março de 2026");
            assertThat(extrator.getTextFromPage(2)).contains("Ocorrências").doesNotContain("Março de 2026");
        }
    }

    @Test void selecionaSomenteMatriculasQueCruzamOPeriodoComLimitesInclusivos() throws Exception {
        var outubro = matricula(7, Periodo.MANHA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        var novembro = matricula(8, Periodo.TARDE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        var dezembro = matricula(9, Periodo.MANHA, LocalDate.of(2026, 12, 1), null);
        cenario(outubro, novembro, dezembro);
        for (int mes = 10; mes <= 12; mes++) {
            var inicio = LocalDate.of(2026, mes, 1);
            var dados = service.montar(10, null, inicio, inicio.withDayOfMonth(inicio.lengthOfMonth()), membro("ADMINISTRADOR"));
            assertThat(dados.matriculas()).extracting(DadosAcompanhamento.MatriculaRelatorio::id).containsExactly(mes - 3);
            verify(frequencias).findPorMatriculasNoPeriodo(List.of(mes - 3), inicio, inicio.withDayOfMonth(inicio.lengthOfMonth()));
            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                    new java.io.ByteArrayInputStream(new RelatorioXlsxGerador().acompanhamento(dados)))) {
                assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            }
        }
        var dados = service.montar(10, null, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), membro("MONITOR"));
        assertThat(dados.matriculas()).extracting(DadosAcompanhamento.MatriculaRelatorio::id).containsExactly(7, 8);
        assertThat(texto(new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados)))
                .contains("01/10/2026 a 31/10/2026", "01/11/2026 a 30/11/2026")
                .doesNotContain("01/12/2026 a hoje");
    }

    @Test void recusaMatriculaEspecificaSemIntersecaoComPeriodoAntesDeConsultarRegistros() {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        for (String perfil : List.of("ADMINISTRADOR", "COORDENADOR", "MONITOR")) {
            for (var formato : br.org.larescolaredencao.model.enums.FormatoRelatorio.values()) {
                assertThatThrownBy(() -> service.gerar(10, 7, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), membro(perfil), formato))
                        .isInstanceOfSatisfying(ResponseStatusException.class,
                                erro -> assertThat(erro.getStatusCode().value()).isEqualTo(400))
                        .hasMessageContaining("fora da vigência da matrícula");
            }
        }
        verifyNoInteractions(frequencias, ocorrencias, entrevistas);
    }

    @Test void permiteIntersecaoParcialEDiasForaDaMatriculaPermanecemNaoAplicaveis() {
        cenario(matricula(7, Periodo.MANHA, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        var dados = service.montar(10, 7, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2), membro("MONITOR"));
        assertThat(dados.situacao(LocalDate.of(2026, 10, 31))).isEqualTo(DadosAcompanhamento.Situacao.SEM_REGISTRO);
        assertThat(dados.situacao(LocalDate.of(2026, 11, 1))).isEqualTo(DadosAcompanhamento.Situacao.SEM_MATRICULA);
        var vazio = service.montar(10, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), membro("MONITOR"));
        assertThat(vazio.matriculas()).isEmpty();
    }

    @Test void monitorExportaFrequenciasDeOutrasUnidadesSemSuasOcorrenciasOuEntrevistas() throws Exception {
        var local = matricula(7, Periodo.MANHA, LocalDate.of(2026, 1, 1), null);
        var externa = matricula(8, Periodo.TARDE, LocalDate.of(2026, 1, 1), null);
        externa.getTurma().getUnidade().setId(4); externa.getTurma().getUnidade().setNome("Outra unidade");
        cenario(local, externa);
        var dia = LocalDate.of(2026, 3, 2);
        when(frequencias.findPorMatriculasNoPeriodo(List.of(7, 8), dia, dia))
                .thenReturn(List.of(freq(local, dia, true), freq(externa, dia, false)));
        when(ocorrencias.listarParaRelatorioAcompanhamento(eq(List.of(7)), any(), any()))
                .thenReturn(List.of(ocorrencia(local, dia.atTime(9, 0), "Ocorrência permitida")));
        var dados = service.montar(10, null, dia, dia, membro("MONITOR"));
        assertThat(dados.registros().get(dia)).hasSize(2);
        assertThat(dados.ocorrencias()).extracting(DadosAcompanhamento.Ocorrencia::idMatricula).containsExactly(7);
        assertThat(dados.incluiEntrevistas()).isFalse();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(new RelatorioXlsxGerador().acompanhamento(dados)))) {
            assertThat(workbook.getSheet("Outra unidade - Tarde").getRow(1).getCell(1).getStringCellValue()).isEqualTo("F");
            assertThat(workbook.getSheet("Entrevistas")).isNull();
        }
        assertThat(texto(new RelatorioAcompanhamentoPdfGerador(new RelatorioPdfGerador()).gerar(dados)))
                .contains("Outra unidade", "Ocorrência permitida").doesNotContain("Entrevistas");
        var somenteExterna = service.montar(10, 8, dia, dia, membro("MONITOR"));
        assertThat(somenteExterna.matriculas()).extracting(DadosAcompanhamento.MatriculaRelatorio::id).containsExactly(8);
        assertThat(somenteExterna.ocorrencias()).isEmpty();
        verify(ocorrencias, times(1)).listarParaRelatorioAcompanhamento(any(), any(), any());
        verifyNoInteractions(entrevistas);
    }

}
