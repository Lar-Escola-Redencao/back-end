package br.org.larescolaredencao.service.relatorio;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class RelatorioXlsxGerador {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String ABA_INFORMACOES = "Informações";

    public byte[] presencas(DadosRelatorio.Presencas dados) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(workbook);

            List<String[]> informacoes = new ArrayList<>(List.of(
                    new String[] {"Relatório", "Presenças detalhadas por período"},
                    new String[] {"Unidade", dados.turma().unidade()},
                    new String[] {"Turma", dados.turma().descricao()}));
            informacoes.addAll(periodo(dados.inicio(), dados.fim(), dados.geradoEm()));
            informacoes.add(new String[] {"Legenda", "P = presente; F = ausente; — = frequência não registrada; "
                    + "N/A = não matriculado na data"});
            if (dados.possuiRegistroForaDaMatricula()) {
                informacoes.add(new String[] {"*", DadosRelatorio.OBSERVACAO_FORA_DA_MATRICULA});
            }
            abaInformacoes(workbook, estilos, informacoes);

            Map<YearMonth, List<LocalDate>> meses = new LinkedHashMap<>();
            dados.datas().forEach(d -> meses.computeIfAbsent(YearMonth.from(d), m -> new ArrayList<>()).add(d));
            for (Map.Entry<YearMonth, List<LocalDate>> mes : meses.entrySet()) {
                Sheet aba = workbook.createSheet(mes.getKey().toString());
                Row cabecalho = aba.createRow(0);
                texto(cabecalho, 0, "Usuário", estilos.cabecalho);
                int coluna = 1;
                for (LocalDate data : mes.getValue()) {
                    texto(cabecalho, coluna, data.format(DIA_MES) + "\n"
                            + data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""),
                            estilos.cabecalhoCentro);
                    aba.setColumnWidth(coluna++, 8 * 256);
                }
                cabecalho.setHeightInPoints(30);

                int linha = 1;
                if (dados.alunos().isEmpty()) {
                    texto(aba.createRow(linha), 0, DadosRelatorio.SEM_ALUNOS, estilos.texto);
                }
                for (DadosRelatorio.AlunoPresencas aluno : dados.alunos()) {
                    Row row = aba.createRow(linha++);
                    texto(row, 0, aluno.aluno().nomeExibicao(), estilos.textoComBorda);
                    coluna = 1;
                    for (LocalDate data : mes.getValue()) {
                        Indicador indicador = aluno.indicadores().get(data);
                        texto(row, coluna++, indicador.getSigla(),
                                indicador.isFalta() ? estilos.falta : estilos.indicador);
                    }
                }
                aba.setColumnWidth(0, 42 * 256);
                aba.createFreezePane(1, 1);
            }

            if (dados.incluiOcorrencias()) {
                abaOcorrencias(workbook, estilos, dados.ocorrencias());
            }
            return escrever(workbook);
        }
    }

    public byte[] resumo(DadosRelatorio.Resumo dados) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(workbook);

            List<String[]> informacoes = new ArrayList<>();
            informacoes.add(new String[] {"Relatório", "Resumo de frequência por período"});
            informacoes.addAll(periodo(dados.inicio(), dados.fim(), dados.geradoEm()));
            informacoes.add(new String[] {"Observação", DadosRelatorio.OBSERVACAO_PERCENTUAL});
            informacoes.add(new String[] {"", ""});
            informacoes.add(new String[] {"Aba", "Unidade / turma"});

            Set<String> nomesUsados = new HashSet<>();
            nomesUsados.add(ABA_INFORMACOES.toLowerCase(PT_BR));
            List<String> nomesAbas = new ArrayList<>();
            for (DadosRelatorio.TurmaResumo turma : dados.turmas()) {
                String sufixo = " - " + turma.turma().abreviacao();
                String unidade = turma.turma().unidade();
                String nome = nomeAbaUnico(unidade.substring(0, Math.min(unidade.length(), 31 - sufixo.length())).trim()
                        + sufixo, nomesUsados);
                nomesAbas.add(nome);
                informacoes.add(new String[] {nome, turma.turma().unidade() + " — " + turma.turma().descricao()});
            }
            abaInformacoes(workbook, estilos, informacoes);

            String[] colunas = {"Usuário", "Presenças", "Faltas", "Total de dias",
                    "Percentual de presença", "Observação"};
            for (int t = 0; t < dados.turmas().size(); t++) {
                DadosRelatorio.TurmaResumo turma = dados.turmas().get(t);
                Sheet aba = workbook.createSheet(nomesAbas.get(t));
                Row cabecalho = aba.createRow(0);
                for (int c = 0; c < colunas.length; c++) {
                    texto(cabecalho, c, colunas[c], estilos.cabecalho);
                }
                int linha = 1;
                for (DadosRelatorio.AlunoResumo aluno : turma.alunos()) {
                    Row row = aba.createRow(linha++);
                    texto(row, 0, aluno.aluno().nomeExibicao(), estilos.textoComBorda);
                    numero(row, 1, aluno.presencas(), estilos.inteiro);
                    numero(row, 2, aluno.faltas(), estilos.inteiro);
                    int numeroLinha = row.getRowNum() + 1;
                    Cell total = row.createCell(3);
                    total.setCellStyle(estilos.inteiro);
                    total.setCellFormula("SUM(B" + numeroLinha + ":C" + numeroLinha + ")");
                    Cell percentual = row.createCell(4);
                    percentual.setCellStyle(estilos.percentual);
                    // Mantém uma casa decimal no percentual, como no PDF, e evita divisão por zero.
                    percentual.setCellFormula("IF(D" + numeroLinha + "=0,\"\",ROUND(B" + numeroLinha + "/D" + numeroLinha + ",3))");
                    texto(row, 5, aluno.observacao(), estilos.textoComBorda);
                }
                if (turma.alunos().isEmpty()) {
                    texto(aba.createRow(linha), 0, DadosRelatorio.SEM_ALUNOS, estilos.texto);
                }
                aba.setColumnWidth(0, 42 * 256);
                aba.setColumnWidth(1, 12 * 256);
                aba.setColumnWidth(2, 10 * 256);
                aba.setColumnWidth(3, 18 * 256);
                aba.setColumnWidth(4, 14 * 256);
                aba.setColumnWidth(5, 44 * 256);
                cabecalho.setHeightInPoints(30);
                aba.createFreezePane(0, 1);
                aba.setAutoFilter(new CellRangeAddress(0, Math.max(0, turma.alunos().size()), 0, colunas.length - 1));
            }
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.setForceFormulaRecalculation(true);
            return escrever(workbook);
        }
    }

    private void abaOcorrencias(Workbook workbook, Estilos estilos, List<DadosRelatorio.Ocorrencia> ocorrencias) {
        Sheet aba = workbook.createSheet("Ocorrências");
        String[] colunas = {"Data e hora", "Usuário", "Tipo", "Descrição", "Autor"};
        Row cabecalho = aba.createRow(0);
        for (int c = 0; c < colunas.length; c++) {
            texto(cabecalho, c, colunas[c], estilos.cabecalho);
        }
        int linha = 1;
        for (DadosRelatorio.Ocorrencia ocorrencia : ocorrencias) {
            Row row = aba.createRow(linha++);
            Cell data = row.createCell(0);
            data.setCellValue(ocorrencia.dataHora());
            data.setCellStyle(estilos.dataHora);
            texto(row, 1, ocorrencia.aluno(), estilos.textoComBorda);
            texto(row, 2, ocorrencia.tipo(), estilos.textoComBorda);
            texto(row, 3, ocorrencia.descricao(), estilos.textoQuebrado);
            texto(row, 4, ocorrencia.autor(), estilos.textoComBorda);
        }
        if (ocorrencias.isEmpty()) {
            texto(aba.createRow(linha), 0, DadosRelatorio.SEM_OCORRENCIAS, estilos.texto);
        } else {
            aba.setAutoFilter(new CellRangeAddress(0, ocorrencias.size(), 0, colunas.length - 1));
        }
        aba.setColumnWidth(0, 18 * 256);
        aba.setColumnWidth(1, 34 * 256);
        aba.setColumnWidth(2, 16 * 256);
        aba.setColumnWidth(3, 70 * 256);
        aba.setColumnWidth(4, 28 * 256);
        aba.createFreezePane(0, 1);
    }

    private void abaInformacoes(Workbook workbook, Estilos estilos, List<String[]> linhas) {
        Sheet aba = workbook.createSheet(ABA_INFORMACOES);
        texto(aba.createRow(0), 0, DadosRelatorio.INSTITUICAO, estilos.titulo);
        int linha = 2;
        for (String[] informacao : linhas) {
            Row row = aba.createRow(linha++);
            texto(row, 0, informacao[0], estilos.rotulo);
            texto(row, 1, informacao[1], estilos.texto);
        }
        aba.setColumnWidth(0, 28 * 256);
        aba.setColumnWidth(1, 100 * 256);
    }

    private List<String[]> periodo(LocalDate inicio, LocalDate fim, java.time.LocalDateTime geradoEm) {
        return List.of(
                new String[] {"Período", inicio.format(DATA) + " a " + fim.format(DATA)},
                new String[] {"Gerado em", geradoEm.format(DATA_HORA) + " (horário de Brasília)"});
    }

    static String nomeAbaUnico(String base, Set<String> usados) {
        String seguro = WorkbookUtil.createSafeSheetName(base.replace("'", "").trim());
        String nome = seguro;
        for (int i = 2; usados.contains(nome.toLowerCase(PT_BR)); i++) {
            String sufixo = " (" + i + ")";
            nome = seguro.substring(0, Math.min(seguro.length(), 31 - sufixo.length())).trim() + sufixo;
        }
        usados.add(nome.toLowerCase(PT_BR));
        return nome;
    }

    /** Textos de cadastro são gravados como célula de texto com prefixo, nunca interpretados como fórmula. */
    private static void texto(Row row, int coluna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor == null ? "" : valor);
        cell.setCellStyle(estilo);
    }

    private static void numero(Row row, int coluna, int valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private static byte[] escrever(Workbook workbook) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        workbook.write(saida);
        return saida.toByteArray();
    }

    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle rotulo;
        final CellStyle texto;
        final CellStyle textoComBorda;
        final CellStyle textoQuebrado;
        final CellStyle cabecalho;
        final CellStyle cabecalhoCentro;
        final CellStyle indicador;
        final CellStyle falta;
        final CellStyle inteiro;
        final CellStyle percentual;
        final CellStyle dataHora;

        Estilos(Workbook workbook) {
            short formatoTexto = workbook.createDataFormat().getFormat("@");
            Font negrito = workbook.createFont();
            negrito.setBold(true);
            Font tituloFonte = workbook.createFont();
            tituloFonte.setBold(true);
            tituloFonte.setFontHeightInPoints((short) 14);

            titulo = workbook.createCellStyle();
            titulo.setFont(tituloFonte);
            titulo.setDataFormat(formatoTexto);
            titulo.setQuotePrefixed(true);

            texto = workbook.createCellStyle();
            texto.setDataFormat(formatoTexto);
            texto.setQuotePrefixed(true);
            texto.setVerticalAlignment(VerticalAlignment.TOP);

            rotulo = workbook.createCellStyle();
            rotulo.cloneStyleFrom(texto);
            rotulo.setFont(negrito);

            textoComBorda = workbook.createCellStyle();
            textoComBorda.cloneStyleFrom(texto);
            bordas(textoComBorda);

            textoQuebrado = workbook.createCellStyle();
            textoQuebrado.cloneStyleFrom(textoComBorda);
            textoQuebrado.setWrapText(true);

            cabecalho = workbook.createCellStyle();
            cabecalho.cloneStyleFrom(textoComBorda);
            cabecalho.setFont(negrito);
            cabecalho.setWrapText(true);
            cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);
            cabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            cabecalhoCentro = workbook.createCellStyle();
            cabecalhoCentro.cloneStyleFrom(cabecalho);
            cabecalhoCentro.setAlignment(HorizontalAlignment.CENTER);

            indicador = workbook.createCellStyle();
            indicador.cloneStyleFrom(textoComBorda);
            indicador.setAlignment(HorizontalAlignment.CENTER);

            falta = workbook.createCellStyle();
            falta.cloneStyleFrom(indicador);
            falta.setFont(negrito);
            falta.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            falta.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            inteiro = workbook.createCellStyle();
            bordas(inteiro);
            inteiro.setDataFormat(workbook.createDataFormat().getFormat("0"));
            inteiro.setAlignment(HorizontalAlignment.CENTER);

            percentual = workbook.createCellStyle();
            bordas(percentual);
            percentual.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
            percentual.setAlignment(HorizontalAlignment.CENTER);

            dataHora = workbook.createCellStyle();
            bordas(dataHora);
            dataHora.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            dataHora.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private static void bordas(CellStyle estilo) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
    }
}
