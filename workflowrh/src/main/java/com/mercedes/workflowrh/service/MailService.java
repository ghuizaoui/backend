package com.mercedes.workflowrh.service;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendHtmlMail(String to, String subject, String htmlBody) throws MessagingException;

    String buildBienvenueMail(String prenom, String nom, String matricule, String motDePasse);



}
