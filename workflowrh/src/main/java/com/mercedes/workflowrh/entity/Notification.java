// src/main/java/com/mercedes/workflowrh/entity/Notification.java
package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notif_demande",      columnList = "demande_id"),
                @Index(name = "idx_notif_destinataire", columnList = "destinataire_id"),
                @Index(name = "idx_notif_statut",       columnList = "statut")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @ManyToOne
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Employe destinataire;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false)
    private String message;

    /** Statut (remplace l'ancien boolean lu) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatutNotification statut;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (statut == null) statut = StatutNotification.NON_LU;
    }
}
