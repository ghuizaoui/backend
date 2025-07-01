package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "validations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    private Demandes demande;

    @ManyToOne
    @JoinColumn(name = "validateur_id", nullable = false)
    private Employe validateur;

    private Integer niveau;

    @Enumerated(EnumType.STRING)
    private StatutValidation statut;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private LocalDateTime dateAction;
}
