package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
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
    private Demandes demande;

    @ManyToOne
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Employe destinataire;

    private String type;

    @Enumerated(EnumType.STRING)
    private StatutNotification statut;

    private LocalDateTime dateEnvoi;
}
