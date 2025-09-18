// src/main/java/com/mercedes/workflowrh/service/DemandeService.java
package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.dto.DemandeDetailDTO;
import com.mercedes.workflowrh.dto.DemandeListDTO;
import com.mercedes.workflowrh.entity.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List; // AJOUTER L'IMPORT


public interface DemandeService {
    Demande createCongeStandard(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin
    );



    Employe getEmployeByMatricule(String matricule);


   List<Demande> getAll();

    Demande createCongeExceptionnel(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin
    );

    Demande createAutorisation(
            TypeDemande typeDemande,

            // PRÉVU (requis)
            LocalDate dateAutorisation,
            LocalTime heureDebut,
            LocalTime heureFin,

            // RÉEL (optionnel)
            LocalDate dateReelle,
            LocalTime heureSortieReelle,
            LocalTime heureRetourReel
    );

    Demande createOrdreMission(
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin,
            String missionObjet
    );
    DemandeDetailDTO findDetail(Long id);

    List<DemandeListDTO> findAllForChef(String matriculeChef);

    List<DemandeListDTO> findAllForDrh();

    Demande validerDemande(Long demandeId, String matriculeValidateur);
    Demande refuserDemande(Long demandeId, String matriculeValidateur, String commentaire);

    List<Demande> getHistoriqueDemandes(String matricule);

    List<Demande> getDemandesEnAttente(String matriculeChef);
    List<Demande> getHistoriqueSubordonnes(String matriculeChef);



    //************************************************** dashboard
    // -------------------- KPI Dashboard --------------------
    long countByStatut(StatutDemande statut);

    long countByCategorie(CategorieDemande categorie);

    long countByDateCreationBetween(LocalDateTime start, LocalDateTime end);

    long countByStatutAndDateCreationBetween(StatutDemande statut, LocalDateTime start, LocalDateTime end);

    List<Object[]> countDemandesByEmploye();

    List<Object[]> countDemandesByService();

    List<Object[]> countDemandesByCategorie();

    Double averageValidationTimeSeconds();

    List<Object[]> countByStatutGrouped();




    // -------------------- Chart / Time-series --------------------
    List<Object[]> countDemandesPerMonth(LocalDateTime start, LocalDateTime end);

    List<Object[]> countDemandesPerMonthAndYear();

    List<Object[]> countDemandesByCategoriePerMonth(LocalDateTime start, LocalDateTime end);

    List<Object[]> countDemandesByType();

    // -------------------- Search / Filter --------------------
    List<Demande> findByEmployeAndStatut(String matricule, StatutDemande statut);

    List<Demande> findByTypeDemande(TypeDemande typeDemande);

    List<Demande> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);

    long countByEmployeAndDateRange(String matricule, LocalDateTime start, LocalDateTime end);

 void  delete(Demande demande);

 Demande getById(long demandeId );

}



