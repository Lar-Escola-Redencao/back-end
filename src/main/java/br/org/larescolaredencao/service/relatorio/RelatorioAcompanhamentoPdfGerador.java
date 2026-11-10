package br.org.larescolaredencao.service.relatorio;

import br.org.larescolaredencao.service.relatorio.DadosAcompanhamento.Situacao;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static br.org.larescolaredencao.service.relatorio.RelatorioPdfGerador.*;

/** PDF individual de acompanhamento: calendários de frequência, ocorrências e (conforme o perfil) entrevistas. */
@Component
public class RelatorioAcompanhamentoPdfGerador {

    private static final String[] MESES = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
            "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
    private static final String[] DIAS = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    private static final int CARDS_POR_LINHA = 3;
    private static final float LARGURA_ESPACO = 8;
    private static final Color FUNDO_SEM_MATRICULA = new Color(210, 210, 210);
    private static final Color CINZA_CLARO = new Color(170, 170, 170);
    private static final Font NUMERO_DIA = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, CINZA_TEXTO);
    private static final Font NUMERO_FORA = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, CINZA_CLARO);
    private static final Font MARCA_NEUTRA = new Font(Font.HELVETICA, 7, Font.NORMAL, CINZA_TEXTO);
    private static final Font MES = new Font(Font.HELVETICA, 9.5f, Font.BOLD);
    private static final Font ANO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font SECAO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font DIA_SEMANA = new Font(Font.HELVETICA, 7, Font.BOLD);

    private final RelatorioPdfGerador base;

    public RelatorioAcompanhamentoPdfGerador(RelatorioPdfGerador base) {
        this.base = base;
    }

    public byte[] gerar(DadosAcompanhamento dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 110, 52);
        PdfWriter writer = PdfWriter.getInstance(documento, saida);
        writer.setPageEvent(new CabecalhoRodape(base.imagemLogo(), "Relatório de acompanhamento", info(dados),
                new Phrase("Dias sem registro de frequência não são contabilizados como faltas.", RODAPE)));
        documento.open();
        float largura = documento.right() - documento.left();

        documento.add(secao("Frequência"));
        if (dados.matriculas().size() > 1) documento.add(legenda(dados));
        for (int ano = dados.inicio().getYear(); ano <= dados.fim().getYear(); ano++) {
            documento.add(calendarioDoAno(dados, ano, largura));
        }

        documento.add(secao("Ocorrências"));
        documento.add(tabelaOcorrencias(dados));
        if (dados.incluiEntrevistas()) {
            documento.add(secao("Entrevistas"));
            documento.add(tabelaEntrevistas(dados));
        }
        documento.close();
        return saida.toByteArray();
    }

    private static String info(DadosAcompanhamento d) {
        return "Usuário: " + d.usuario() + "\nPeríodo: " + d.inicio().format(DATA) + " a " + d.fim().format(DATA)
                + "\nMatrícula: " + d.filtroMatricula()
                + "\nEmitido em " + d.geradoEm().format(DATA_HORA) + " (horário de Brasília) por " + d.responsavel();
    }

    private Paragraph secao(String titulo) {
        Paragraph p = new Paragraph(titulo, SECAO);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        p.setKeepTogether(true);
        return p;
    }

    private Paragraph legenda(DadosAcompanhamento dados) {
        Paragraph legenda = new Paragraph();
        legenda.setLeading(13);
        legenda.setSpacingAfter(6);
        legenda.add(new Chunk("Matrículas: ", INFO));
        if (dados.matriculas().isEmpty()) {
            legenda.add(new Chunk("nenhuma matrícula acessível.", INFO));
        }
        for (var m : dados.matriculas()) {
            String texto = "M" + m.id() + " = " + m.unidade() + ", " + m.turma()
                    + (m.ingresso() == null ? "" : " (desde " + m.ingresso().format(DATA)
                    + (m.desligamento() == null ? "" : " até " + m.desligamento().format(DATA)) + ")");
            legenda.add(new Chunk(texto + (m == dados.matriculas().get(dados.matriculas().size() - 1) ? "." : "; "), INFO));
        }
        if (dados.matriculas().size() > 1) {
            legenda.add(new Chunk("\nQuando há mais de uma matrícula, cada registro do dia indica sua matrícula (ex.: M"
                    + dados.matriculas().get(0).id() + " P).", INFO));
        }
        return legenda;
    }

    private PdfPTable calendarioDoAno(DadosAcompanhamento dados, int ano, float larguraUtil) {
        YearMonth primeiro = YearMonth.of(ano, 1).isBefore(YearMonth.from(dados.inicio()))
                ? YearMonth.from(dados.inicio()) : YearMonth.of(ano, 1);
        YearMonth ultimo = YearMonth.of(ano, 12).isAfter(YearMonth.from(dados.fim()))
                ? YearMonth.from(dados.fim()) : YearMonth.of(ano, 12);
        int total = ultimo.getMonthValue() - primeiro.getMonthValue() + 1;

        // Grade de 6 colunas: cada card ocupa 2, de modo que linhas incompletas ficam centralizadas.
        PdfPTable tabela = new PdfPTable(6);
        tabela.setWidthPercentage(100);
        tabela.setSplitLate(true);
        tabela.setSplitRows(false);
        tabela.setHeaderRows(1);
        PdfPCell titulo = new PdfPCell(new Phrase(String.valueOf(ano), ANO));
        titulo.setColspan(6);
        titulo.setBorder(Rectangle.BOTTOM);
        titulo.setBorderColor(CINZA_BORDA);
        titulo.setPaddingBottom(3);
        titulo.setPaddingLeft(0);
        tabela.addCell(titulo);

        float larguraCard = (larguraUtil - 2 * LARGURA_ESPACO * CARDS_POR_LINHA) / CARDS_POR_LINHA;
        for (int i = 0; i < total; i += CARDS_POR_LINHA) {
            int naLinha = Math.min(CARDS_POR_LINHA, total - i);
            int vazias = CARDS_POR_LINHA - naLinha; // 0, 1 ou 2 cards ausentes => 0, 1 ou 2 colunas de cada lado
            if (vazias > 0) tabela.addCell(vazia(vazias));
            for (int k = 0; k < naLinha; k++) {
                PdfPCell envoltorio = new PdfPCell(cardDoMes(dados, primeiro.plusMonths(i + k), larguraCard));
                envoltorio.setColspan(2);
                envoltorio.setBorder(Rectangle.NO_BORDER);
                envoltorio.setPadding(LARGURA_ESPACO);
                envoltorio.setPaddingBottom(LARGURA_ESPACO + 2);
                tabela.addCell(envoltorio);
            }
            if (vazias > 0) tabela.addCell(vazia(vazias));
        }
        return tabela;
    }

    /** Colunas vazias de cada lado: metade do espaço de um card por card ausente. */
    private PdfPCell vazia(int cardsAusentes) {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        c.setColspan(cardsAusentes); // 1 card ausente => 1 coluna; 2 ausentes => 2 colunas, uma de cada lado de 2
        return c;
    }

    private PdfPTable cardDoMes(DadosAcompanhamento dados, YearMonth mes, float largura) {
        PdfPTable card = new PdfPTable(7);
        card.setTotalWidth(largura);
        card.setLockedWidth(true);
        card.setSplitLate(false);

        PdfPCell nome = new PdfPCell(new Phrase(MESES[mes.getMonthValue() - 1] + " de " + mes.getYear(), MES));
        nome.setColspan(7);
        estiloCabecalho(nome, Element.ALIGN_CENTER);
        card.addCell(nome);
        for (String dia : DIAS) {
            PdfPCell c = new PdfPCell(new Phrase(dia, DIA_SEMANA));
            estiloCabecalho(c, Element.ALIGN_CENTER);
            c.setPadding(2);
            c.setBackgroundColor(new Color(240, 240, 240));
            card.addCell(c);
        }
        int deslocamento = mes.atDay(1).getDayOfWeek().getValue() % 7; // domingo = 0
        for (int i = 0; i < deslocamento; i++) card.addCell(semDia());
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            card.addCell(celulaDia(dados, mes.atDay(dia)));
        }
        int resto = (deslocamento + mes.lengthOfMonth()) % 7;
        for (int i = 0; resto != 0 && i < 7 - resto; i++) card.addCell(semDia());
        return card;
    }

    private PdfPCell semDia() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBorder(Rectangle.NO_BORDER);
        c.setMinimumHeight(30);
        return c;
    }

    private PdfPCell celulaDia(DadosAcompanhamento dados, LocalDate data) {
        Situacao situacao = dados.situacao(data);
        String numero = String.valueOf(data.getDayOfMonth());
        Phrase conteudo = new Phrase(13);
        Color fundo = Color.WHITE;
        switch (situacao) {
            case FORA_DO_PERIODO -> conteudo.add(new Chunk(numero, NUMERO_FORA));
            case SEM_MATRICULA -> {
                fundo = FUNDO_SEM_MATRICULA;
                conteudo.add(new Chunk(numero + "\n", NUMERO_DIA));
                conteudo.add(new Chunk("—", MARCA_NEUTRA));
            }
            case SEM_REGISTRO -> {
                conteudo.add(new Chunk(numero + "\n", NUMERO_DIA));
                conteudo.add(new Chunk("—", MARCA_NEUTRA));
            }
            case COM_REGISTRO -> {
                var registros = dados.registros().get(data);
                boolean todosPresentes = registros.stream().allMatch(DadosAcompanhamento.RegistroDia::presente);
                boolean todosFaltas = registros.stream().noneMatch(DadosAcompanhamento.RegistroDia::presente);
                boolean identificar = dados.matriculas().size() > 1;
                // Fundo da célula só quando todos os registros do dia coincidem; senão cada linha é destacada.
                if (todosPresentes) fundo = FUNDO_PRESENCA;
                if (todosFaltas) fundo = FUNDO_FALTA;
                boolean uniforme = todosPresentes || todosFaltas;
                conteudo.add(new Chunk(numero, uniforme && todosFaltas ? new Font(Font.HELVETICA, 6.5f, Font.NORMAL, Color.WHITE)
                        : NUMERO_DIA));
                for (var r : registros) {
                    String texto = (identificar ? "M" + r.idMatricula() + " " : "") + (r.presente() ? "P" : "F");
                    Font fonte = r.presente() ? (identificar ? new Font(Font.HELVETICA, 7, Font.BOLD, VERDE_PRESENCA)
                            : INDICADOR_PRESENCA)
                            : (uniforme ? (identificar ? new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE) : INDICADOR_FALTA)
                            : new Font(Font.HELVETICA, 7, Font.BOLD, Color.WHITE));
                    Chunk marca = new Chunk(texto, fonte);
                    if (!uniforme) marca.setBackground(r.presente() ? FUNDO_PRESENCA : FUNDO_FALTA, 1, 1, 1, 1);
                    conteudo.add(new Chunk("\n", fonte));
                    conteudo.add(marca);
                }
            }
        }
        PdfPCell c = new PdfPCell(conteudo);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2);
        c.setMinimumHeight(30);
        c.setBackgroundColor(fundo);
        c.setBorderColor(CINZA_BORDA);
        c.setBorderWidth(0.5f);
        return c;
    }

    private PdfPTable tabelaOcorrencias(DadosAcompanhamento dados) {
        PdfPTable tabela = tabela(new float[] {10, 9, 12, 29, 16, 24});
        for (String coluna : List.of("Data", "Horário", "Tipo", "Descrição", "Autor", "Unidade e turma")) {
            tabela.addCell(cabecalho(coluna, Element.ALIGN_LEFT));
        }
        if (dados.ocorrencias().isEmpty()) {
            tabela.addCell(vazio(DadosAcompanhamento.SEM_OCORRENCIAS, 6));
        }
        for (var o : dados.ocorrencias()) {
            tabela.addCell(texto(o.dataHora().format(DATA)));
            tabela.addCell(texto(o.dataHora().format(HORA)));
            tabela.addCell(texto(o.tipo()));
            tabela.addCell(texto(o.descricao()));
            tabela.addCell(texto(o.autor()));
            tabela.addCell(texto(o.unidadeTurma()));
        }
        return tabela;
    }

    private PdfPTable tabelaEntrevistas(DadosAcompanhamento dados) {
        PdfPTable tabela = tabela(new float[] {11, 9, 57, 23});
        for (String coluna : List.of("Data", "Horário", "Descrição", "Autor")) {
            tabela.addCell(cabecalho(coluna, Element.ALIGN_LEFT));
        }
        if (dados.entrevistas().isEmpty()) {
            tabela.addCell(vazio(DadosAcompanhamento.SEM_ENTREVISTAS, 4));
        }
        for (var e : dados.entrevistas()) {
            tabela.addCell(texto(e.dataHora().format(DATA)));
            tabela.addCell(texto(e.dataHora().format(HORA)));
            tabela.addCell(texto(e.descricao()));
            tabela.addCell(texto(e.autor()));
        }
        return tabela;
    }

    private PdfPTable tabela(float[] larguras) {
        PdfPTable tabela = new PdfPTable(larguras);
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(8);
        tabela.setHeaderRows(1);
        // Descrições longas continuam na página seguinte em vez de serem cortadas.
        tabela.setSplitLate(false);
        tabela.setSplitRows(true);
        return tabela;
    }

    private PdfPCell vazio(String mensagem, int colunas) {
        PdfPCell c = celula(mensagem, DESTAQUE, Element.ALIGN_LEFT);
        c.setColspan(colunas);
        return c;
    }

    /** Preserva parágrafos e quebras de linha da descrição original. */
    private PdfPCell texto(String conteudo) {
        String normalizado = conteudo == null ? "" : conteudo.replace("\r\n", "\n").replace('\r', '\n')
                .replace("\t", "    ");
        PdfPCell c = celula(normalizado, TEXTO, Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        return c;
    }
}
