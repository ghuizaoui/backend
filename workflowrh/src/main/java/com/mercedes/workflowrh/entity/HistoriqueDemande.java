// src/main/java/com/mercedes/workflowrh/entity/HistoriqueDemande.java
package com.mercedes.workflowrh.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "historique_demandes",
        indexes = {
                @Index(name = "idx_hist_demande_id",        columnList = "demande_id"),
                @Index(name = "idx_hist_employe_matricule", columnList = "employe_matricule"),
                @Index(name = "idx_hist_validateur_mat",    columnList = "validateur_matricule"),
                @Index(name = "idx_hist_action",            columnList = "action"),
                @Index(name = "idx_hist_date",              columnList = "date_action"),
                @Index(name = "idx_hist_statut",            columnList = "statut")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"demande"})
public class HistoriqueDemande {

    public static final String VERBE_CREEE      = "Créée";
    public static final String VERBE_MODIFIEE   = "Modifiée";
    public static final String VERBE_SUPPRIMEE  = "Supprimée";
    public static final String VERBE_VALIDEE    = "Validée";
    public static final String VERBE_REFUSEE    = "Refusée";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    @JsonIgnore
    private Demande demande;

    @Column(name = "employe_matricule", nullable = false, length = 20)
    private String employeMatricule;

    @Column(name = "employe_nom_prenom", length = 200)
    private String employeNomPrenom;

    @Column(name = "validateur_matricule", length = 20)
    private String validateurMatricule;

    @Column(name = "validateur_nom_prenom", length = 200)
    private String validateurNomPrenom;

    @Column(nullable = false, length = 150)
    private String action; // ex: "Congé standard - Créée"

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(columnDefinition = "TEXT")
    private String commentaire; // motif si Refusée/Annulée

    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutDemande statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieDemande categorie;

    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande;

    @Column(name = "date_creation_demande")
    private LocalDateTime dateCreationDemande;

    @Column(name = "date_validation_demande")
    private LocalDateTime dateValidationDemande;

    private Long versionDemande;

    // -------- Snapshot métiers --------

    // Congé
    private LocalDate congeDateDebut;
    private LocalDate congeDateFin;
    private LocalTime congeHeureDebut;
    private LocalTime congeHeureFin;

    // Autorisation (nouveau modèle : un seul jour)
    private LocalDate autoDate;                 // jour prévu
    private LocalTime autoHeureDebut;           // heure début prévue
    private LocalTime autoHeureFin;             // heure fin prévue

    private LocalDate autoDateReelle;           // le plus souvent = autoDate
    private LocalTime autoHeureSortieReelle;    // heure sortie réelle
    private LocalTime autoHeureRetourReel;      // heure retour réelle

    // Ordre de mission
    private LocalDate missionDateDebut;
    private LocalTime missionHeureDebut;
    private LocalDate missionDateFin;
    private LocalTime missionHeureFin;

    @Column(columnDefinition = "TEXT")
    private String missionObjet;

    @PrePersist
    void prePersist() {
        if (dateAction == null) dateAction = LocalDateTime.now();
    }

    // ---------- Factories ----------
    public static HistoriqueDemande creerHistoriqueCreation(Demande d) {
        return base(d, composeAction(VERBE_CREEE, d)).build().copier(d);
    }

    public static HistoriqueDemande creerHistoriqueModification(Demande d) {
        return base(d, composeAction(VERBE_MODIFIEE, d)).build().copier(d);
    }

    public static HistoriqueDemande creerHistoriqueSuppression(Demande d) {
        return base(d, composeAction(VERBE_SUPPRIMEE, d)).build().copier(d);
    }

    public static HistoriqueDemande creerHistoriqueValidation(Demande d) {
        return base(d, composeAction(VERBE_VALIDEE, d)).build().copier(d);
    }

    public static HistoriqueDemande creerHistoriqueRefus(Demande d, String motif) {
        return base(d, composeAction(VERBE_REFUSEE, d)).commentaire(motif).build().copier(d);
    }

    // ---------- Outils ----------
    private static HistoriqueDemande.HistoriqueDemandeBuilder base(Demande d, String actionLibelle) {
        return HistoriqueDemande.builder()
                .demande(d)
                .action(actionLibelle)
                .dateAction(LocalDateTime.now())
                .workflowId(d.getWorkflowId());
    }

    private static String composeAction(String verbe, Demande d) {
        return libelleCategorie(d.getCategorie()) + " - " + verbe;
    }

    private static String libelleCategorie(CategorieDemande c) {
        return switch (c) {
            case CONGE_STANDARD     -> "Congé standard";
            case CONGE_EXCEPTIONNEL -> "Congé exceptionnel";
            case AUTORISATION       -> "Autorisation";
            case ORDRE_MISSION      -> "Ordre de mission";
        };
    }

    private HistoriqueDemande copier(Demande d) {
        if (d.getEmploye() != null) {
            this.employeMatricule = d.getEmploye().getMatricule();
            this.employeNomPrenom = concatNP(d.getEmploye().getNom(), d.getEmploye().getPrenom());
        }
        if (d.getValidateur() != null) {
            this.validateurMatricule = d.getValidateur().getMatricule();
            this.validateurNomPrenom = concatNP(d.getValidateur().getNom(), d.getValidateur().getPrenom());
        }

        this.statut = d.getStatut();
        this.categorie = d.getCategorie();
        this.typeDemande = d.getTypeDemande();
        this.dateCreationDemande = d.getDateCreation();
        this.dateValidationDemande = d.getDateValidation();
        this.versionDemande = d.getVersion();

        // Congé
        this.congeDateDebut   = d.getCongeDateDebut();
        this.congeDateFin     = d.getCongeDateFin();
        this.congeHeureDebut  = d.getCongeHeureDebut();
        this.congeHeureFin    = d.getCongeHeureFin();

        // Autorisation (nouveau modèle)
        this.autoDate               = d.getAutoDate();
        this.autoHeureDebut         = d.getAutoHeureDebut();
        this.autoHeureFin           = d.getAutoHeureFin();
        this.autoDateReelle         = d.getAutoDateReelle();
        this.autoHeureSortieReelle  = d.getAutoHeureSortieReelle();
        this.autoHeureRetourReel    = d.getAutoHeureRetourReel();

        // Ordre de mission
        this.missionDateDebut = d.getMissionDateDebut();
        this.missionHeureDebut= d.getMissionHeureDebut();
        this.missionDateFin   = d.getMissionDateFin();
        this.missionHeureFin  = d.getMissionHeureFin();
        this.missionObjet     = d.getMissionObjet();

        return this;
    }

    @JsonIgnore
    private static String concatNP(String nom, String prenom) {
        String n  = (nom == null ? "" : nom.trim());
        String pn = (prenom == null ? "" : prenom.trim());
        return (n + " " + pn).trim();
    }
}
