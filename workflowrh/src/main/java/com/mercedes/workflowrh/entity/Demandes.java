package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demandes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @Column(columnDefinition = "TEXT")
    private String commentaireRefus;



    private String interimMatricule;

    private LocalDateTime dateCreation;



    @OneToMany(mappedBy = "demandes")
    private List<HistoriqueDemandes> historiqueDemandes;

    @OneToMany(mappedBy = "demandes")
    private List<Notification> notifications;


}
