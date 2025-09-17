// src/main/java/com/mercedes/workflowrh/service/impl/NotificationServiceImpl.java
package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.NotificationPayload;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.NotificationRepository;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeRepository employeRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MailService mailService;

    // ---- Formatage FR
    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter D_DAY  = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
    private static final DateTimeFormatter D_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", FR);
    private static final DateTimeFormatter T_HM   = DateTimeFormatter.ofPattern("HH:mm", FR);

    @Override @Transactional
    public void notifyManagerOfNewDemand(Demande d) {
        Employe creator = d.getEmploye();
        if (creator == null) return;

        Set<String> destinataires = new LinkedHashSet<>();

        if (creator.getRole() == Role.EMPLOYE) {
            addIfNotBlank(destinataires, creator.getChefHierarchique1Matricule());
            addIfNotBlank(destinataires, creator.getChefHierarchique2Matricule());

            if (destinataires.isEmpty()) {
                employeRepository.findByRole(Role.CHEF)
                        .forEach(c -> addIfNotBlank(destinataires, c.getMatricule()));
            }
        } else if (creator.getRole() == Role.CHEF) {
            employeRepository.findByRole(Role.DRH)
                    .forEach(drh -> addIfNotBlank(destinataires, drh.getMatricule()));
        }

        if (destinataires.isEmpty()) return;

        String subject = "Nouvelle demande";

        // 🔴 ICI : message personnalisé par destinataire
        destinataires.forEach(matriculeDest -> {
            String message = buildMessageCreationForRecipient(d, matriculeDest);
            sendToMatricule(d, matriculeDest, subject, message);
        });
    }

    // --- helpers existants (addIfNotBlank, afterCommit, etc.) ---

    /** Retourne le message "création" adapté au destinataire. */
    private String buildMessageCreationForRecipient(Demande d, String destinataireMatricule) {
        Employe creator = d.getEmploye();
        boolean toOwner = creator != null
                && creator.getMatricule() != null
                && creator.getMatricule().equals(destinataireMatricule);

        return toOwner ? buildMessageCreationOwner(d) : buildMessageCreationForManager(d);
    }

    private String fullName(Employe e) {
        if (e == null) return "";
        String nom = e.getNom() != null ? e.getNom() : "";
        String prenom = e.getPrenom() != null ? e.getPrenom() : "";
        return (nom + " " + prenom).trim();
    }

    /** Texte pour l’AUTEUR (“Votre …”) — reprend ta logique existante. */
    private String buildMessageCreationOwner(Demande d) {
        if (d.getCategorie() == CategorieDemande.AUTORISATION && d.getAutoDate()!=null) {
            String h1 = d.getAutoHeureDebut()!=null ? T_HM.format(d.getAutoHeureDebut()) : null;
            String h2 = d.getAutoHeureFin()!=null ? T_HM.format(d.getAutoHeureFin()) : null;
            return "Votre autorisation a été créée pour le "
                    + D_DAY.format(d.getAutoDate())
                    + (h1!=null ? " de " + h1 + (h2!=null ? " à " + h2 : "") : "")
                    + ".";
        }
        if ((d.getCategorie()==CategorieDemande.CONGE_STANDARD || d.getCategorie()==CategorieDemande.CONGE_EXCEPTIONNEL)
                && d.getCongeDateDebut()!=null) {
            return "Votre demande " + libelleType(d.getTypeDemande())
                    + " a été créée pour le " + D_DAY.format(d.getCongeDateDebut()) + ".";
        }
        if (d.getCategorie()==CategorieDemande.ORDRE_MISSION && d.getMissionDateDebut()!=null && d.getMissionDateFin()!=null) {
            return "Votre demande de mission a été créée du "
                    + D_DAY.format(d.getMissionDateDebut()) + " au " + D_DAY.format(d.getMissionDateFin()) + ".";
        }
        return "Votre demande a été créée.";
    }

    /** Texte pour CHEF/DRH (“Nom Prénom a créé …”). */
    private String buildMessageCreationForManager(Demande d) {
        String auteur = fullName(d.getEmploye());
        if (d.getCategorie() == CategorieDemande.AUTORISATION && d.getAutoDate()!=null) {
            String h1 = d.getAutoHeureDebut()!=null ? T_HM.format(d.getAutoHeureDebut()) : null;
            String h2 = d.getAutoHeureFin()!=null ? T_HM.format(d.getAutoHeureFin()) : null;
            return auteur + " a créé une autorisation pour le "
                    + D_DAY.format(d.getAutoDate())
                    + (h1!=null ? " de " + h1 + (h2!=null ? " à " + h2 : "") : "")
                    + ".";
        }
        if ((d.getCategorie()==CategorieDemande.CONGE_STANDARD || d.getCategorie()==CategorieDemande.CONGE_EXCEPTIONNEL)
                && d.getCongeDateDebut()!=null) {
            String periode = D_DAY.format(d.getCongeDateDebut());
            if (d.getCongeDateFin()!=null && !d.getCongeDateFin().isEqual(d.getCongeDateDebut())) {
                periode += " → " + D_DAY.format(d.getCongeDateFin());
            }
            return auteur + " a créé une demande de " + libelleType(d.getTypeDemande())
                    + " (" + periode + ").";
        }
        if (d.getCategorie()==CategorieDemande.ORDRE_MISSION && d.getMissionDateDebut()!=null && d.getMissionDateFin()!=null) {
            return auteur + " a créé un ordre de mission du "
                    + D_DAY.format(d.getMissionDateDebut()) + " au " + D_DAY.format(d.getMissionDateFin()) + ".";
        }
        return auteur + " a créé une demande.";
    }
    // --- le reste de tes méthodes (notifyEmployeeOnValidation/Refuse, markAsRead, etc.) reste inchangé ---

    private static void addIfNotBlank(Set<String> set, String matricule) {
        if (matricule != null && !matricule.isBlank()) set.add(matricule);
    }


    // ✅ nouvelle méthode
    @Override @Transactional
    public void notifyEmployeeOnCreation(Demande d) {
        Employe creator = d.getEmploye();
        if (creator == null) return;
        sendToMatricule(d, creator.getMatricule(),
                "Demande créée", buildMessageCreation(d));
    }

    @Override @Transactional
    public void notifyEmployeeOnValidation(Demande d) {
        Employe creator = d.getEmploye();
        if (creator == null) return;
        sendToMatricule(d, creator.getMatricule(),
                "Demande validée", buildMessageValidation(d));
    }

    @Override @Transactional
    public void notifyEmployeeOnRefuse(Demande d) {
        Employe creator = d.getEmploye();
        if (creator == null) return;
        sendToMatricule(d, creator.getMatricule(),
                "Demande refusée", buildMessageRefus(d));
    }

    @Override @Transactional
    public void markAsRead(Long notifId, String matricule) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            if (n.getDestinataire()!=null && matricule.equals(n.getDestinataire().getMatricule())) {
                n.setStatut(StatutNotification.LU);
                notificationRepository.save(n);
            }
        });
    }

    // ---- helpers ----------------------------------------------------------

    private void sendToMatricule(Demande d, String destinataireMatricule, String subject, String message) {
        Employe dest = (destinataireMatricule == null || destinataireMatricule.isBlank())
                ? null : employeRepository.findByMatricule(destinataireMatricule).orElse(null);
        if (dest == null) return;

        // 1) Persistance dans la transaction
        Notification n = Notification.builder()
                .demande(d)
                .destinataire(dest)
                .subject(subject)
                .message(message)
                .statut(StatutNotification.NON_LU)
                .dateCreation(LocalDateTime.now())
                .build();
        notificationRepository.save(n);

        // 2) Payload à envoyer après commit
        NotificationPayload payload = NotificationPayload.builder()
                .id(n.getId())
                .demandeId(d.getId())
                .type(typeFor(d))
                .subject(subject)
                .message(message)
                .statut(n.getStatut().name())
                .dateCreation(n.getDateCreation())
                .dateValidation(d.getDateValidation())
                .motifRefus(d.getCommentaireRefus())
                .categorie(d.getCategorie()!=null ? d.getCategorie().name() : null)
                .typeDemande(d.getTypeDemande()!=null ? d.getTypeDemande().name() : null)
                .auteurMatricule(d.getEmploye()!=null ? d.getEmploye().getMatricule() : null)
                .destinataire(destinataireMatricule)
                .build();

        afterCommit(() -> {
            try {
                simpMessagingTemplate.convertAndSendToUser(destinataireMatricule, "/queue/notifications", payload);
            } catch (Exception e) {
                log.warn("WS notif échouée pour {}: {}", destinataireMatricule, e.getMessage());
            }

            if (dest.getEmail() != null && !dest.getEmail().isBlank()) {
                try {
                    String html = buildEmailHtml(subject, message, d);
                    mailService.sendHtmlMail(dest.getEmail(), subject, html);
                } catch (MessagingException e) {
                    log.warn("Échec e-mail notif à {}: {}", dest.getEmail(), e.getMessage());
                }
            }
        });
    }

    private void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { r.run(); }
            });
        } else {
            r.run();
        }
    }
    private String typeFor(Demande d) {
        StatutDemande s = (d != null) ? d.getStatut() : null;
        if (s == null) return "DEMANDE_UPDATED";
        return switch (s) {
            case EN_COURS -> "DEMANDE_CREATED";
            case VALIDEE  -> "DEMANDE_VALIDATED";
            case REFUSEE  -> "DEMANDE_REFUSED";
            default       -> "DEMANDE_UPDATED";
        };
    }

    // --------- Construction de messages FR (normalisés) --------------------

    private String libelleType(TypeDemande td) {
        if (td == null) return "demande";
        return switch (td) {
            case CONGE_ANNUEL -> "congé annuel";
            case CONGE_SANS_SOLDE -> "congé sans solde";
            case CONGE_REPOS_COMPENSATEUR -> "repos compensateur";
            // complète selon tes types...
            default -> td.name().replace("_"," ").toLowerCase(FR);
        };
    }

    private String buildMessageCreation(Demande d) {
        if (d.getCategorie() == CategorieDemande.AUTORISATION && d.getAutoDate()!=null) {
            String h1 = d.getAutoHeureDebut()!=null ? T_HM.format(d.getAutoHeureDebut()) : null;
            String h2 = d.getAutoHeureFin()!=null ? T_HM.format(d.getAutoHeureFin()) : null;
            return "Votre autorisation a été créée pour le "
                    + D_DAY.format(d.getAutoDate())
                    + (h1!=null ? " de " + h1 + (h2!=null ? " à " + h2 : "") : "")
                    + ".";
        }
        if ((d.getCategorie()==CategorieDemande.CONGE_STANDARD || d.getCategorie()==CategorieDemande.CONGE_EXCEPTIONNEL)
                && d.getCongeDateDebut()!=null) {
            return "Votre demande " + libelleType(d.getTypeDemande())
                    + " a été créée pour le " + D_DAY.format(d.getCongeDateDebut()) + ".";
        }
        if (d.getCategorie()==CategorieDemande.ORDRE_MISSION && d.getMissionDateDebut()!=null && d.getMissionDateFin()!=null) {
            return "Votre demande de mission a été créée du "
                    + D_DAY.format(d.getMissionDateDebut()) + " au " + D_DAY.format(d.getMissionDateFin()) + ".";
        }
        return "Votre demande a été créée.";
    }

    private String buildMessageValidation(Demande d) {
        if (d.getCategorie() == CategorieDemande.AUTORISATION && d.getAutoDate()!=null) {
            String h1 = d.getAutoHeureDebut()!=null ? T_HM.format(d.getAutoHeureDebut()) : null;
            String h2 = d.getAutoHeureFin()!=null ? T_HM.format(d.getAutoHeureFin()) : null;
            return "Votre autorisation a été validée pour le "
                    + D_DAY.format(d.getAutoDate())
                    + (h1!=null ? " de " + h1 + (h2!=null ? " à " + h2 : "") : "")
                    + ".";
        }
        if ((d.getCategorie()==CategorieDemande.CONGE_STANDARD || d.getCategorie()==CategorieDemande.CONGE_EXCEPTIONNEL)
                && d.getCongeDateDebut()!=null) {
            return "Votre demande " + libelleType(d.getTypeDemande())
                    + " a été validée pour le " + D_DAY.format(d.getCongeDateDebut()) + ".";
        }
        if (d.getCategorie()==CategorieDemande.ORDRE_MISSION && d.getMissionDateDebut()!=null && d.getMissionDateFin()!=null) {
            return "Votre demande de mission a été validée du "
                    + D_DAY.format(d.getMissionDateDebut()) + " au " + D_DAY.format(d.getMissionDateFin()) + ".";
        }
        return "Votre demande a été validée.";
    }

    private String buildMessageRefus(Demande d) {
        String base = "Votre demande " + libelleType(d.getTypeDemande()) + " a été refusée.";
        String motif = d.getCommentaireRefus();
        return (motif!=null && !motif.isBlank()) ? base + " Motif : " + motif : base;
    }

    // --------- Périodes pour le payload ------------------------------------

    private LocalDate periodDebut(Demande d) {
        return switch (d.getCategorie()) {
            case AUTORISATION   -> d.getAutoDate();
            case ORDRE_MISSION  -> d.getMissionDateDebut();
            case CONGE_STANDARD, CONGE_EXCEPTIONNEL -> d.getCongeDateDebut();
        };
    }

    private LocalDate periodFin(Demande d) {
        return switch (d.getCategorie()) {
            case AUTORISATION   -> d.getAutoDate();
            case ORDRE_MISSION  -> d.getMissionDateFin();
            case CONGE_STANDARD, CONGE_EXCEPTIONNEL -> d.getCongeDateFin();
        };
    }

    private LocalDateTime asStartOfDay(LocalDate d) {
        return d!=null ? d.atStartOfDay() : null;
    }

    // --------- E-mail HTML (mêmes textes + date FR) ------------------------

    private String buildEmailHtml(String subject, String message, Demande d) {
        String when = (d!=null && d.getDateValidation()!=null)
                ? D_FULL.format(d.getDateValidation())
                : (d!=null && d.getDateCreation()!=null ? D_FULL.format(d.getDateCreation()) : "");

        StringBuilder extra = new StringBuilder();
        if (d != null) {
            if ((d.getCategorie()==CategorieDemande.CONGE_STANDARD || d.getCategorie()==CategorieDemande.CONGE_EXCEPTIONNEL)
                    && d.getCongeDateDebut()!=null) {
                extra.append("<p><b>Période :</b> ").append(D_DAY.format(d.getCongeDateDebut()));
                if (d.getCongeDateFin()!=null) extra.append(" → ").append(D_DAY.format(d.getCongeDateFin()));
                extra.append("</p>");
            } else if (d.getCategorie()==CategorieDemande.AUTORISATION && d.getAutoDate()!=null) {
                extra.append("<p><b>Jour :</b> ").append(D_DAY.format(d.getAutoDate())).append("</p>");
                if (d.getAutoHeureDebut()!=null && d.getAutoHeureFin()!=null) {
                    extra.append("<p><b>Plage :</b> ")
                            .append(T_HM.format(d.getAutoHeureDebut()))
                            .append(" - ")
                            .append(T_HM.format(d.getAutoHeureFin()))
                            .append("</p>");
                }
            } else if (d.getCategorie()==CategorieDemande.ORDRE_MISSION && d.getMissionDateDebut()!=null && d.getMissionDateFin()!=null) {
                extra.append("<p><b>Période :</b> ")
                        .append(D_DAY.format(d.getMissionDateDebut()))
                        .append(" → ")
                        .append(D_DAY.format(d.getMissionDateFin()))
                        .append("</p>");
            }

            if (d.getCommentaireRefus()!=null && !d.getCommentaireRefus().isBlank()) {
                extra.append("<p><b>Motif :</b> ").append(d.getCommentaireRefus()).append("</p>");
            }
        }

        return """
               <html><body style="font-family:system-ui, -apple-system, Segoe UI, Roboto, sans-serif; color:#222">
                 <h3 style="margin:0 0 8px 0;">%s</h3>
                 <p style="margin:0 0 4px 0;">%s</p>
                 %s
                 <p style="margin:8px 0 0 0; color:#6b7280; font-size:12px;">
                   <span style="display:inline-block; margin-right:6px;">🕒</span>%s
                 </p>
                 <hr style="border:none;border-top:1px solid #eee; margin:12px 0"/>
                 <small style="color:#6b7280">Notification automatique – Workflow RH</small>
               </body></html>
               """.formatted(subject, message, extra.toString(), when);
    }

    private String safe(String s){ return s==null? "": s; }
}
