package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remetente;

    /**
     * Sem MAIL_USERNAME configurado (ambiente local, ou antes de configurar em produção),
     * só registra o link no log em vez de tentar enviar de verdade — evita quebrar o fluxo
     * de "esqueci minha senha" por falta de credenciais de SMTP.
     */
    public void enviarRedefinicaoSenha(String destinatario, String link) {
        // .trim() porque é comum sobrar um espaço ou quebra de linha ao colar a variável
        // de ambiente numa plataforma de deploy — isso quebra o parser de endereço de e-mail.
        String remetenteLimpo = remetente == null ? "" : remetente.trim();

        if (remetenteLimpo.isBlank()) {
            log.info("MAIL_USERNAME não configurado — envio de e-mail simulado. Link de redefinição para {}: {}",
                    destinatario, link);
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetenteLimpo);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Redefinir senha — BarberPro");
        mensagem.setText("""
                Recebemos um pedido para redefinir sua senha no BarberPro.

                Clique no link abaixo para criar uma nova senha (válido por 1 hora):
                %s

                Se você não pediu isso, pode ignorar este e-mail — sua senha continua a mesma.
                """.formatted(link));

        mailSender.send(mensagem);
    }
}
