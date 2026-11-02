package br.org.larescolaredencao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailServiceTest {

    @Test
    void deveEnviarAgradecimentoComHtmlLogoValorEmReaisEAlternativaTexto() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "remetente", "contato@example.org");

        service.enviarAgradecimentoPix("doador@example.org", new BigDecimal("125.50"));
        message.saveChanges();

        verify(mailSender).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("doador@example.org");
        assertThat(message.getSubject()).isEqualTo("Obrigado pelo apoio — Lar Escola Redenção");
        Part html = encontrarParte(message, "text/html");
        assertThat(html).isNotNull();
        assertThat((String) html.getContent())
                .contains("Obrigado pela sua doação!", "125,50", "Contribuição informada via Pix",
                        "cid:logoImage", "não confirma o recebimento", "#2A4FCC");
        Part texto = encontrarParte(message, "text/plain");
        assertThat(texto).isNotNull();
        assertThat((String) texto.getContent()).contains("125,50", "Não confirma o recebimento");
        Part imagem = encontrarParte(message, "image/png");
        assertThat(imagem).isNotNull();
        assertThat(imagem.getHeader("Content-ID")).contains("<logoImage>");
    }

    private Part encontrarParte(Part parte, String tipo) throws Exception {
        if (parte.isMimeType(tipo)) return parte;
        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int indice = 0; indice < multipart.getCount(); indice++) {
                Part encontrada = encontrarParte(multipart.getBodyPart(indice), tipo);
                if (encontrada != null) return encontrada;
            }
        }
        return null;
    }
}
