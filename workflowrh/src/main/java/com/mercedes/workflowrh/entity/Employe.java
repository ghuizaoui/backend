package com.mercedes.workflowrh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "employes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String matricule;

    @Column(nullable = false)
    private String motDePasse;

    private String nom;
    private String prenom;
    private String direction;
    private String service;
    private Integer grade;

    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    private TypeContrat typeContrat;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "chef_1_id")
    private Employe chef1;

    @ManyToOne
    @JoinColumn(name = "chef_2_id")
    private Employe chef2;

    @OneToMany(mappedBy = "employe")
    private List<Demande> demandes;

    @OneToMany(mappedBy = "employe")
    private List<SoldeConge> soldesConges;

    @OneToMany(mappedBy = "destinataire")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "acteur")
    private List<HistoriqueDemande> historiqueDemandes;

    @OneToMany(mappedBy = "validateur")
    private List<Validation> validations;
}
