package com.mercedes.workflowrh.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"historiques", "notifications"})
@Table(name = "demandes")
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employe_id", referencedColumnName = "matricule", nullable = false)
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "interimaire_id", referencedColumnName = "matricule")
    private Employe interimaire; // Nouvel attribut

    @Column(name = "pas_d_interim")
    private Boolean pasDInterim; // Nouvel attribut pour la case à cocher

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemande statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieDemande categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_demande", length = 50)
    private TypeDemande typeDemande;

    @ManyToOne
    @JoinColumn(name = "validateur_id", referencedColumnName = "matricule")
    private Employe validateur;

    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;

    @Column(columnDefinition = "TEXT")
    private String commentaireRefus;

    private String workflowId;

    private LocalDate congeDateDebut;
    private LocalDate congeDateFin;
    private LocalTime congeHeureDebut;
    private LocalTime congeHeureFin;

    // src/main/java/com/mercedes/workflowrh/entity/Demande.java
// ...
    // Jour prévu + plage prévue
    private LocalDate autoDate;        // le seul jour de l'autorisation
    private LocalTime autoHeureDebut;  // heure début prévue
    private LocalTime autoHeureFin;    // heure fin prévue

    private LocalDate autoDateReelle;
    private LocalDate autoDateFin;
    private LocalTime autoHeureSortieReelle;

    private LocalTime autoHeureRetourReel;


    private LocalDateTime dateDemande;


    private LocalDate missionDateDebut;
    private LocalTime missionHeureDebut;
    private LocalDate missionDateFin;
    private LocalTime missionHeureFin;

    @Column(columnDefinition = "TEXT")
    private String missionObjet;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<HistoriqueDemande> historiques;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications;

    @Version
    private Long version;


    // NEW FIELD: Liberation status for concierge
    @Column(name = "est_liberer", nullable = false)
    @Builder.Default
    private Boolean estLiberer = false;


    @Lob
    @Column(name = "file", columnDefinition = "LONGBLOB")
    private byte[] file;


    @PrePersist
    void prePersist() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (dateDemande == null) dateDemande = LocalDateTime.now();
        if (statut == null) statut = StatutDemande.EN_COURS;
        syncCategorieEtType();
    }

    @PreUpdate
    void preUpdate() {
        syncCategorieEtType();
    }

    private void syncCategorieEtType() {
        if (categorie == CategorieDemande.ORDRE_MISSION) {
            typeDemande = null;
        } else if (typeDemande != null) {
            categorie = typeDemande.getCategorie();
        }
    }

    @JsonIgnore public boolean isCongeStandard()    { return categorie == CategorieDemande.CONGE_STANDARD; }
    @JsonIgnore public boolean isCongeExceptionnel(){ return categorie == CategorieDemande.CONGE_EXCEPTIONNEL; }
    @JsonIgnore public boolean isAutorisation()     { return categorie == CategorieDemande.AUTORISATION; }
    @JsonIgnore public boolean isOrdreMission()     { return categorie == CategorieDemande.ORDRE_MISSION; }
}