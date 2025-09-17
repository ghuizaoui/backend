package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public void sendHtmlMail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        javaMailSender.send(message);
    }

    // Helper pour générer le mail avec Thymeleaf (optionnel)
    public String buildBienvenueMail(String prenom, String nom, String matricule, String motDePasse) {
        Context context = new Context();
        context.setVariable("prenom", prenom);
        context.setVariable("nom", nom);
        context.setVariable("matricule", matricule);
        context.setVariable("motDePasse", motDePasse);
        return templateEngine.process("mail_bienvenue.html", context);
    }



}
