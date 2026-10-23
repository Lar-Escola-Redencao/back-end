package br.org.larescolaredencao.service.relatorio;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RelatorioPdfGerador {

    // Cada mês é dividido nos blocos 01–16 e 17–último dia.
    static final int ULTIMO_DIA_PRIMEIRO_BLOCO = 16;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color CINZA_BORDA = new Color(150, 150, 150);
    private static final Color CINZA_CABECALHO = new Color(225, 225, 225);
    private static final Color VERDE_PRESENCA = new Color(27, 94, 32);
    private static final Color FUNDO_PRESENCA = new Color(220, 237, 200);
    private static final Color VERMELHO_FALTA = new Color(183, 28, 28);
    private static final Color FUNDO_FALTA = new Color(255, 205, 210);
    private static final Color CINZA_TEXTO = new Color(80, 80, 80);
    private static final Color FAIXA_ALTA = new Color(46, 125, 50);
    private static final Color FAIXA_MEDIA = new Color(240, 170, 30);
    private static final Color FAIXA_BAIXA = new Color(198, 40, 40);
    private static final Color TRILHA_BARRA = new Color(235, 235, 235);

    private static final Font TITULO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font INFO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, CINZA_TEXTO);
    private static final Font CABECALHO = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font INDICADOR = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font INDICADOR_PRESENCA = new Font(Font.HELVETICA, 10, Font.BOLD, VERDE_PRESENCA);
    private static final Font INDICADOR_FALTA = new Font(Font.HELVETICA, 10, Font.BOLD, VERMELHO_FALTA);
    private static final Font TEXTO_PRESENCA = new Font(Font.HELVETICA, 9, Font.BOLD, VERDE_PRESENCA);
    private static final Font TEXTO_FALTA = new Font(Font.HELVETICA, 9, Font.BOLD, VERMELHO_FALTA);
    private static final Font INDICADOR_SECUNDARIO = new Font(Font.HELVETICA, 8, Font.NORMAL, CINZA_TEXTO);
    private static final Font RODAPE = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, CINZA_TEXTO);
    private static final Font DESTAQUE = new Font(Font.HELVETICA, 9, Font.ITALIC, CINZA_TEXTO);

    private static final String CAMINHO_LOGO = "static/images/logo.png";
    private static final float ALTURA_LOGO = 30;

    private final BufferedImage logo = logoEmTonsDeCinza();

    record BlocoDatas(String rotulo, List<LocalDate> datas) {}

    public byte[] presencas(DadosRelatorio.Presencas dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 62, 44);
        PdfWriter writer = PdfWriter.getInstance(documento, saida);
        String legenda = "Legenda: P = presente  ·  F = ausente  ·  — = frequência não registrada  ·  N/A = não matriculado na data"
                + (dados.possuiRegistroForaDaMatricula() ? "  ·  * = registro fora da vigência da matrícula, mantido como salvo" : "");
        writer.setPageEvent(new CabecalhoRodape(imagemLogo(), "Presenças detalhadas por período",
                infoTurma(dados.turma(), dados.inicio(), dados.fim(), dados.geradoEm()), legenda));
        documento.open();

        List<BlocoDatas> blocos = blocos(dados.datas());
        for (int i = 0; i < blocos.size(); i++) {
            // Cada bloco de datas começa em página nova e só termina após todos os alunos.
            if (i > 0) documento.newPage();
            documento.add(tabelaBloco(blocos.get(i), dados.alunos(), documento.right() - documento.left()));
        }

        if (dados.incluiOcorrencias()) {
            documento.newPage();
            documento.add(tabelaOcorrencias(dados));
        }
        documento.close();
        return saida.toByteArray();
    }

    public byte[] resumo(DadosRelatorio.Resumo dados) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 76, 52);
        PdfWriter writer = PdfWriter.getInstance(documento, saida);
        CabecalhoRodape cabecalho = new CabecalhoRodape(imagemLogo(), "Resumo de frequência por período", "",
                DadosRelatorio.OBSERVACAO_PERCENTUAL);
        writer.setPageEvent(cabecalho);
        documento.open();

        for (int i = 0; i < dados.turmas().size(); i++) {
            DadosRelatorio.TurmaResumo turma = dados.turmas().get(i);
            // O cabeçalho só muda depois do fechamento da última página da turma anterior.
            if (i > 0) documento.newPage();
            cabecalho.info = infoTurma(turma.turma(), dados.inicio(), dados.fim(), dados.geradoEm());
            documento.add(tabelaResumo(turma));
            documento.add(legendaFaixas());
            Paragraph observacao = new Paragraph(DadosRelatorio.OBSERVACAO_PERCENTUAL, DESTAQUE);
            observacao.setSpacingBefore(6);
            documento.add(observacao);
            if (turma.alunos().stream().anyMatch(DadosRelatorio.AlunoResumo::possuiRegistroForaDaMatricula)) {
                documento.add(new Paragraph("* " + DadosRelatorio.OBSERVACAO_FORA_DA_MATRICULA, DESTAQUE));
            }
        }
        documento.close();
        return saida.toByteArray();
    }

    static List<BlocoDatas> blocos(List<LocalDate> datas) {
        List<BlocoDatas> blocos = new ArrayList<>();
        List<LocalDate> atual = new ArrayList<>();
        for (LocalDate data : datas) {
            if (!atual.isEmpty() && !mesmoBloco(atual.get(0), data)) {
                blocos.add(new BlocoDatas(rotulo(atual), atual));
                atual = new ArrayList<>();
            }
            atual.add(data);
        }
        if (!atual.isEmpty()) blocos.add(new BlocoDatas(rotulo(atual), atual));
        return blocos;
    }

    private static boolean mesmoBloco(LocalDate a, LocalDate b) {
        return a.getYear() == b.getYear() && a.getMonth() == b.getMonth()
                && (a.getDayOfMonth() <= ULTIMO_DIA_PRIMEIRO_BLOCO) == (b.getDayOfMonth() <= ULTIMO_DIA_PRIMEIRO_BLOCO);
    }

    static String rotulo(List<LocalDate> datas) {
        LocalDate inicio = datas.get(0);
        LocalDate fim = datas.get(datas.size() - 1);
        String mes = inicio.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + " de " + inicio.getYear();
        return inicio.equals(fim)
                ? String.format("%02d de %s", inicio.getDayOfMonth(), mes)
                : String.format("%02d – %02d de %s", inicio.getDayOfMonth(), fim.getDayOfMonth(), mes);
    }

    private PdfPTable tabelaBloco(BlocoDatas bloco, List<DadosRelatorio.AlunoPresencas> alunos, float larguraUtil) {
        float larguraData = 34;
        float larguraNome = larguraUtil - ULTIMO_DIA_PRIMEIRO_BLOCO * larguraData;
        float[] larguras = new float[bloco.datas().size() + 1];
        larguras[0] = larguraNome;
        for (int i = 1; i < larguras.length; i++) larguras[i] = larguraData;

        PdfPTable tabela = new PdfPTable(larguras.length);
        tabela.setTotalWidth(larguras);
        tabela.setLockedWidth(true);
        tabela.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabela.setHeaderRows(2);

        PdfPCell titulo = celula(bloco.rotulo(), TITULO, Element.ALIGN_LEFT);
        titulo.setColspan(larguras.length);
        titulo.setBorder(Rectangle.NO_BORDER);
        titulo.setPaddingBottom(6);
        tabela.addCell(titulo);

        tabela.addCell(cabecalho("Aluno", Element.ALIGN_LEFT));
        for (LocalDate data : bloco.datas()) {
            Phrase texto = new Phrase(String.format("%02d", data.getDayOfMonth()), CABECALHO);
            texto.add(new Chunk("\n" + data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""),
                    INDICADOR_SECUNDARIO));
            PdfPCell celula = new PdfPCell(texto);
            estiloCabecalho(celula, Element.ALIGN_CENTER);
            tabela.addCell(celula);
        }

        if (alunos.isEmpty()) {
            PdfPCell vazio = celula(DadosRelatorio.SEM_ALUNOS, DESTAQUE, Element.ALIGN_LEFT);
            vazio.setColspan(larguras.length);
            tabela.addCell(vazio);
        }
        for (DadosRelatorio.AlunoPresencas aluno : alunos) {
            tabela.addCell(celula(aluno.aluno().nomeExibicao(), TEXTO, Element.ALIGN_LEFT));
            for (LocalDate data : bloco.datas()) {
                Indicador indicador = aluno.indicadores().get(data);
                Font fonte = indicador.isPresenca() ? INDICADOR_PRESENCA
                        : indicador.isFalta() ? INDICADOR_FALTA : INDICADOR_SECUNDARIO;
                PdfPCell celula = celula(indicador.getSigla(), fonte, Element.ALIGN_CENTER);
                if (indicador.isPresenca()) celula.setBackgroundColor(FUNDO_PRESENCA);
                if (indicador.isFalta()) celula.setBackgroundColor(FUNDO_FALTA);
                tabela.addCell(celula);
            }
        }
        return tabela;
    }

    private PdfPTable tabelaOcorrencias(DadosRelatorio.Presencas dados) {
        PdfPTable tabela = new PdfPTable(new float[] {12, 19, 11, 43, 15});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(2);
        // Descrições longas continuam na página seguinte em vez de serem cortadas.
        tabela.setSplitLate(false);
        tabela.setSplitRows(true);

        PdfPCell titulo = celula("Ocorrências — " + dados.inicio().format(DATA) + " a " + dados.fim().format(DATA),
                TITULO, Element.ALIGN_LEFT);
        titulo.setColspan(5);
        titulo.setBorder(Rectangle.NO_BORDER);
        titulo.setPaddingBottom(6);
        tabela.addCell(titulo);
        for (String coluna : List.of("Data e hora", "Aluno", "Tipo", "Descrição", "Autor")) {
            tabela.addCell(cabecalho(coluna, Element.ALIGN_LEFT));
        }

        if (dados.ocorrencias().isEmpty()) {
            PdfPCell vazio = celula(DadosRelatorio.SEM_OCORRENCIAS, DESTAQUE, Element.ALIGN_LEFT);
            vazio.setColspan(5);
            tabela.addCell(vazio);
        }
        for (DadosRelatorio.Ocorrencia ocorrencia : dados.ocorrencias()) {
            tabela.addCell(celula(ocorrencia.dataHora().format(DATA_HORA), TEXTO, Element.ALIGN_LEFT));
            tabela.addCell(celula(ocorrencia.aluno(), TEXTO, Element.ALIGN_LEFT));
            tabela.addCell(celula(ocorrencia.tipo(), TEXTO, Element.ALIGN_LEFT));
            tabela.addCell(celula(ocorrencia.descricao(), TEXTO, Element.ALIGN_LEFT));
            tabela.addCell(celula(ocorrencia.autor(), TEXTO, Element.ALIGN_LEFT));
        }
        return tabela;
    }

    private PdfPTable tabelaResumo(DadosRelatorio.TurmaResumo turma) {
        PdfPTable tabela = new PdfPTable(new float[] {38, 12, 10, 14, 26});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        tabela.addCell(cabecalho("Aluno", Element.ALIGN_LEFT));
        tabela.addCell(cabecalho("Presenças", Element.ALIGN_CENTER));
        tabela.addCell(cabecalho("Faltas", Element.ALIGN_CENTER));
        tabela.addCell(cabecalho("Dias com frequência registrada", Element.ALIGN_CENTER));
        tabela.addCell(cabecalho("Presença (%)", Element.ALIGN_LEFT));

        if (turma.alunos().isEmpty()) {
            PdfPCell vazio = celula(DadosRelatorio.SEM_ALUNOS, DESTAQUE, Element.ALIGN_LEFT);
            vazio.setColspan(5);
            tabela.addCell(vazio);
        }
        for (DadosRelatorio.AlunoResumo aluno : turma.alunos()) {
            String nome = aluno.aluno().nomeExibicao() + (aluno.possuiRegistroForaDaMatricula() ? " *" : "");
            tabela.addCell(celula(nome, TEXTO, Element.ALIGN_LEFT));
            tabela.addCell(celula(String.valueOf(aluno.presencas()), TEXTO_PRESENCA, Element.ALIGN_CENTER));
            tabela.addCell(celula(String.valueOf(aluno.faltas()), TEXTO_FALTA, Element.ALIGN_CENTER));
            tabela.addCell(celula(String.valueOf(aluno.diasRegistrados()), TEXTO, Element.ALIGN_CENTER));

            BigDecimal percentual = aluno.percentualArredondado();
            if (percentual == null) {
                tabela.addCell(celula(DadosRelatorio.SEM_FREQUENCIA, DESTAQUE, Element.ALIGN_LEFT));
            } else {
                PdfPCell celula = celula(formatarPercentual(percentual), CABECALHO, Element.ALIGN_LEFT);
                celula.setCellEvent(new BarraPercentual(aluno.percentual()));
                tabela.addCell(celula);
            }
        }
        return tabela;
    }

    private Paragraph legendaFaixas() {
        Paragraph legenda = new Paragraph();
        legenda.setSpacingBefore(8);
        legenda.add(new Chunk("Barra de presença:  ", INFO));
        adicionarFaixa(legenda, FAIXA_ALTA, "75% ou mais");
        adicionarFaixa(legenda, FAIXA_MEDIA, "de 50% a 74,9%");
        adicionarFaixa(legenda, FAIXA_BAIXA, "abaixo de 50%");
        legenda.add(new Chunk("O comprimento da barra corresponde ao percentual.", INFO));
        return legenda;
    }

    private void adicionarFaixa(Paragraph legenda, Color cor, String texto) {
        Chunk amostra = new Chunk("\u00a0\u00a0\u00a0\u00a0", INFO);
        amostra.setBackground(cor);
        legenda.add(amostra);
        legenda.add(new Chunk(" " + texto + "     ", INFO));
    }

    static String formatarPercentual(BigDecimal percentual) {
        return percentual.toPlainString().replace('.', ',') + "%";
    }

    static Color corFaixa(double percentual) {
        if (percentual >= 0.75) return FAIXA_ALTA;
        if (percentual >= 0.5) return FAIXA_MEDIA;
        return FAIXA_BAIXA;
    }

    /** A logo é colorida; no relatório vai em tons de cinza, preservando a transparência. */
    private static BufferedImage logoEmTonsDeCinza() {
        try (InputStream entrada = new ClassPathResource(CAMINHO_LOGO).getInputStream()) {
            BufferedImage original = ImageIO.read(entrada);
            BufferedImage cinza = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    int argb = original.getRGB(x, y);
                    int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                    int luminancia = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                    cinza.setRGB(x, y, (argb & 0xff000000) | (luminancia << 16) | (luminancia << 8) | luminancia);
                }
            }
            return cinza;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar a logo dos relatórios", e);
        }
    }

    private Image imagemLogo() {
        try {
            Image imagem = Image.getInstance(logo, null);
            imagem.scaleToFit(ALTURA_LOGO * logo.getWidth() / logo.getHeight(), ALTURA_LOGO);
            return imagem;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String infoTurma(DadosRelatorio.Turma turma, LocalDate inicio, LocalDate fim,
                                    java.time.LocalDateTime geradoEm) {
        return "Unidade: " + turma.unidade() + "  ·  Turma: " + turma.descricao()
                + "  ·  Período: " + inicio.format(DATA) + " a " + fim.format(DATA)
                + "  ·  Gerado em " + geradoEm.format(DATA_HORA) + " (horário de Brasília)";
    }

    private static PdfPCell celula(String texto, Font fonte, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte));
        celula.setHorizontalAlignment(alinhamento);
        celula.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celula.setPadding(4);
        celula.setBorderColor(CINZA_BORDA);
        celula.setBorderWidth(0.5f);
        return celula;
    }

    private static PdfPCell cabecalho(String texto, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, CABECALHO));
        estiloCabecalho(celula, alinhamento);
        return celula;
    }

    private static void estiloCabecalho(PdfPCell celula, int alinhamento) {
        celula.setHorizontalAlignment(alinhamento);
        celula.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celula.setPadding(4);
        celula.setBackgroundColor(CINZA_CABECALHO);
        celula.setBorderColor(CINZA_BORDA);
        celula.setBorderWidth(0.5f);
    }

    /** Desenha a barra à direita do número; o número continua legível em impressão preto e branco. */
    private record BarraPercentual(double percentual) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle posicao, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            float esquerda = posicao.getLeft() + 50;
            float largura = posicao.getRight() - 6 - esquerda;
            float altura = 8;
            float base = (posicao.getTop() + posicao.getBottom()) / 2 - altura / 2;
            canvas.saveState();
            canvas.setColorFill(TRILHA_BARRA);
            canvas.rectangle(esquerda, base, largura, altura);
            canvas.fill();
            canvas.setColorFill(corFaixa(percentual));
            canvas.rectangle(esquerda, base, (float) (largura * percentual), altura);
            canvas.fill();
            canvas.setLineWidth(0.5f);
            canvas.setColorStroke(Color.BLACK);
            canvas.rectangle(esquerda, base, largura, altura);
            canvas.stroke();
            canvas.restoreState();
        }
    }

    private static final class CabecalhoRodape extends PdfPageEventHelper {
        private final Image logo;
        private final String relatorio;
        private final String rodape;
        private String info;

        CabecalhoRodape(Image logo, String relatorio, String info, String rodape) {
            this.logo = logo;
            this.relatorio = relatorio;
            this.info = info;
            this.rodape = rodape;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            PdfContentByte canvas = writer.getDirectContent();
            float topo = documento.getPageSize().getTop() - 24;
            logo.setAbsolutePosition(documento.left(), topo - ALTURA_LOGO + 12);
            canvas.addImage(logo);
            float esquerdaTexto = documento.left() + logo.getScaledWidth() + 12;
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(DadosRelatorio.INSTITUICAO + " — " + relatorio, TITULO), esquerdaTexto, topo, 0);
            ColumnText informacoes = new ColumnText(canvas);
            informacoes.setSimpleColumn(esquerdaTexto, topo - 30, documento.right(), topo - 4);
            informacoes.setLeading(10.5f);
            informacoes.addText(new Phrase(info, INFO));
            informacoes.go();

            float base = documento.bottom() - 22;
            ColumnText rodapeTexto = new ColumnText(canvas);
            rodapeTexto.setSimpleColumn(documento.left(), base - 14, documento.right() - 70, base + 10);
            rodapeTexto.addText(new Phrase(rodape, RODAPE));
            rodapeTexto.go();
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), RODAPE), documento.right(), base, 0);
        }
    }
}
