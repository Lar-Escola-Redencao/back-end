package br.org.larescolaredencao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarEmailRecuperacao(String destinatario, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("Código de Recuperação de Senha - Lar Escola Redenção");

            String htmlContent = """
                    <!DOCTYPE html>
                    <html lang="pt-BR">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #eef1f6; margin: 0; padding: 0; }
                            .wrapper { width: 100%%; background-color: #eef1f6; padding: 32px 16px; }
                            .container { max-width: 480px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(9, 46, 94, 0.08); }
                            .header { background-color: #2A4FCC; padding: 28px 0; text-align: center; }
                            .header img { max-width: 96px; max-height: 96px; object-fit: contain; display: block; margin: 0 auto; border-radius: 8px; background-color: #ffffff; padding: 6px; }
                            .content { padding: 36px 32px 28px 32px; color: #33373d; text-align: center; }
                            .content h1 { font-size: 20px; font-weight: 600; color: #14213d; margin: 0 0 12px 0; }
                            .content p { margin: 0 0 4px 0; font-size: 15px; line-height: 1.6; color: #5c6270; }
                            .label { display: block; margin: 28px 0 10px 0; font-size: 12px; font-weight: 600; letter-spacing: 1.5px; text-transform: uppercase; color: #8891a5; }
                            .code-box { display: inline-block; font-family: 'Consolas', 'Courier New', monospace; font-size: 34px; font-weight: 700; color: #14213d; background-color: #eef4fd; padding: 16px 32px; border-radius: 10px; letter-spacing: 10px; border: 1.5px solid #c6d8f7; }
                            .timer-pill { display: inline-block; margin-top: 18px; font-size: 13px; font-weight: 500; color: #a35a0f; background-color: #fdf1de; padding: 7px 16px; border-radius: 20px; }
                            .divider { border: none; border-top: 1px solid #eef0f3; margin: 28px 32px 0 32px; }
                            .footer { padding: 20px 32px 28px 32px; text-align: center; }
                            .footer p { margin: 0; font-size: 12px; color: #a3a9b7; line-height: 1.6; }
                            @media (max-width: 480px) {
                                .code-box { font-size: 26px; letter-spacing: 6px; padding: 14px 20px; }
                                .content { padding: 28px 20px 20px 20px; }
                            }
                        </style>
                    </head>
                    <body>
                        <div class="wrapper">
                            <div class="container">
                                <div class="header">
                                    <img src="cid:logoImage" alt="Lar Escola Redenção">
                                </div>
                                <div class="content">
                                    <h1>Recuperação de senha</h1>
                                    <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                                    <p>Use o código abaixo para continuar:</p>

                                    <span class="label">Seu código de verificação</span>
                                    <div class="code-box">%s</div>

                                    <br>
                                    <span class="timer-pill">Válido por 15 minutos</span>
                                </div>
                                <hr class="divider">
                                <div class="footer">
                                    <p>Se você não solicitou essa alteração, pode ignorar este e-mail com segurança.<br>
                                    Este é um e-mail automático — não responda.</p>
                                    <p style="margin-top: 10px;">© 2026 Lar Escola Redenção. Todos os direitos reservados.</p>
                                </div>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(codigo);

            helper.setText(htmlContent, true);
            
            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            if (logo.exists()) {
                helper.addInline("logoImage", logo);
            }
            
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            logger.error("Falha ao enviar e-mail de recuperação para {}: {}", destinatario, e.getMessage(), e);
        } 
    }
}