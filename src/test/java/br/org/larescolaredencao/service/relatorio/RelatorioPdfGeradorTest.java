package br.org.larescolaredencao.service.relatorio;

import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioPdfGeradorTest {
    @Test void descreveOPeriodoPorExtensoSomenteNoTituloDasOcorrencias() throws Exception {
        var inicio = LocalDate.of(2026, 1, 1);
        var fim = LocalDate.of(2027, 1, 1);
        var dados = new DadosRelatorio.Presencas(
                new DadosRelatorio.Turma("Sede São José", "Manhã", "Manhã"),
                inicio, fim, inicio.atTime(12, 0), List.of(fim), List.of(), List.of(), false);
        try (var reader = new PdfReader(new RelatorioPdfGerador().presencas(dados))) {
            var extrator = new PdfTextExtractor(reader);
            assertThat(extrator.getTextFromPage(1)).contains("01 de janeiro de 2027", "01/01/2026 a 01/01/2027");
            assertThat(extrator.getTextFromPage(2))
                    .contains("Ocorrências de 01 de janeiro de 2026 - 01 de janeiro de 2027");
        }
    }

    @Test void preservaAsCoresDaLogoEAsInformacoesDoCabecalhoNosDoisModelos() throws Exception {
        var turma = new DadosRelatorio.Turma("Sede São José", "Manhã (07:00 - 12:30)", "Manhã");
        var dia = LocalDate.of(2026, 10, 29);
        var gerado = dia.atTime(11, 44);
        var gerador = new RelatorioPdfGerador();
        var detalhado = new DadosRelatorio.Presencas(turma, dia, dia, gerado, List.of(dia),
                List.of(new DadosRelatorio.AlunoPresencas(new DadosRelatorio.Aluno("Arthur", false),
                        Map.of(dia, Indicador.FALTA))), null, false);
        var resumo = new DadosRelatorio.Resumo(dia, dia, gerado,
                List.of(new DadosRelatorio.TurmaResumo(turma, List.of())));
        for (byte[] pdf : List.of(gerador.presencas(detalhado), gerador.resumo(resumo))) {
            try (var reader = new PdfReader(pdf)) {
                var texto = new PdfTextExtractor(reader).getTextFromPage(1);
                assertThat(texto).contains("Sede São José", "Manhã", "29/10/2026", "11:44", "Brasília");
                var imagens = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT);
                boolean possuiCor = false;
                for (PdfName chave : imagens.getKeys()) {
                    var objeto = PdfReader.getPdfObject(imagens.get(chave));
                    if (objeto instanceof PRStream stream && PdfName.DEVICERGB.equals(stream.getAsName(PdfName.COLORSPACE))) {
                        byte[] pixels = PdfReader.getStreamBytes(stream);
                        for (int i = 0; i + 2 < pixels.length; i += 3) {
                            if (pixels[i] != pixels[i + 1] || pixels[i + 1] != pixels[i + 2]) { possuiCor = true; break; }
                        }
                    }
                }
                assertThat(possuiCor).as("A logo deve manter os canais de cor originais").isTrue();
            }
        }
    }
}
