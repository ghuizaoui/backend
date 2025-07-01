package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_demandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueDemandes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    private Demandes demande;

    private String action;

    @ManyToOne
    @JoinColumn(name = "acteur_id", nullable = false)
    private Employe acteur;

    private LocalDateTime dateAction;
}
