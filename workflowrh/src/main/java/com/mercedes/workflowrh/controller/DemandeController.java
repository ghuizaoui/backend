// src/main/java/com/mercedes/workflowrh/controller/DemandeController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.*;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.service.DemandeService;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mercedes.workflowrh.security.AppUserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;
    private  final SoldeCongeService soldeCongeService;
    private static final Logger logger = Logger.getLogger(DemandeController.class.getName());
    private final EmployeService employeService;


    @PostMapping("/conge-standard")
    public ResponseEntity<Demande> createCongeStandard(@Valid @RequestBody CongeRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {

            if(req.getTypeDemande()!=TypeDemande.CONGE_SANS_SOLDE) {
                logger.info("Creating CONGE_STANDARD demand for user: " + matricule);
                // Check SoldeConge balance
                long joursPris = ChronoUnit.DAYS.between(req.getDateDebut(), req.getDateFin()) + 1;
                Employe employe = demandeService.getEmployeByMatricule(matricule); // Assume this method exists
                float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule()).get().getSoldeActuel();
                if (soldeActuel < joursPris) {
                    logger.warning("Insufficient balance for CONGE_STANDARD: required " + joursPris + ", available " + soldeActuel);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(null); // Return null with 400 status for insufficient balance
                }
            }
            Demande demande = demandeService.createCongeStandard(
                    req.getTypeDemande(),
                    req.getDateDebut(), req.getHeureDebut(),
                    req.getDateFin(), req.getHeureFin()
            );
            logger.info("Successfully created CONGE_STANDARD demand with ID: " + demande.getId());
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            logger.severe("Error creating CONGE_STANDARD demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/conge-exceptionnel")
    public ResponseEntity<Demande> createCongeExceptionnel(@Valid @RequestBody CongeRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
           Demande demande = demandeService.createCongeExceptionnel(
                    req.getTypeDemande(),
                    req.getDateDebut(), req.getHeureDebut(),
                    req.getDateFin(), req.getHeureFin()
            );
            logger.info("Successfully created CONGE_EXCEPTIONNEL demand with ID: " + demande.getId());
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            logger.severe("Error creating CONGE_EXCEPTIONNEL demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/autorisation")
    public ResponseEntity<Demande> createAutorisation(@Valid @RequestBody AutorisationRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Creating AUTORISATION demand for user: " + matricule);
            // Check SoldeConge balance
            LocalTime debut = req.getHeureSortieReelle() != null ? req.getHeureSortieReelle() : req.getHeureDebut();
            LocalTime fin = req.getHeureRetourReel() != null ? req.getHeureRetourReel() : req.getHeureFin();
            if (debut != null && fin != null) {
                long minutes = ChronoUnit.MINUTES.between(debut, fin);
                double joursPris = (minutes / 240.0) * 0.5;
                Employe employe = demandeService.getEmployeByMatricule(matricule); // Assume this method exists
                float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule()).get().getSoldeActuel();
                if (soldeActuel < joursPris) {
                    logger.warning("Insufficient balance for AUTORISATION: required " + joursPris + ", available " + soldeActuel);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(null); // Return null with 400 status for insufficient balance
                }
            }
            Demande demande = demandeService.createAutorisation(
                    req.getTypeDemande(),
                    req.getDateAutorisation(),
                    req.getHeureDebut(),
                    req.getHeureFin(),
                    req.getDateReelle(),
                    req.getHeureSortieReelle(),
                    req.getHeureRetourReel()
            );
            logger.info("Successfully created AUTORISATION demand with ID: " + demande.getId());
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            logger.severe("Error creating AUTORISATION demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/ordre-mission")
    public ResponseEntity<Demande> createOrdreMission(@Valid @RequestBody OrdreMissionRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Creating ORDRE_MISSION demand for user: " + matricule);
            // No SoldeConge balance check for ORDRE_MISSION
            Demande demande = demandeService.createOrdreMission(
                    req.getDateDebut(), req.getHeureDebut(),
                    req.getDateFin(), req.getHeureFin(),
                    req.getMissionObjet()
            );
            logger.info("Successfully created ORDRE_MISSION demand with ID: " + demande.getId());
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            logger.severe("Error creating ORDRE_MISSION demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }




    @PostMapping("/validation/{demandeId}")
    public ResponseEntity<Demande> validerRefuserDemande(
            @PathVariable Long demandeId,
            @RequestBody ValidationRequest validationRequest) {

        // Récupérer l'employé connecté (validateur)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matriculeValidateur = auth.getName();

        Demande demandeMiseAJour;

        if (validationRequest.getIsValidee()) {
            // Valider la demande
            demandeMiseAJour = demandeService.validerDemande(demandeId, matriculeValidateur);

            //  Mise à jour du solde congé si la demande est validée
            Employe employe = demandeMiseAJour.getEmploye();
            if (employe != null) {
                if (demandeMiseAJour.getCategorie() == CategorieDemande.CONGE_STANDARD ||
                        demandeMiseAJour.getCategorie() == CategorieDemande.CONGE_EXCEPTIONNEL) {
                    // Calcul des jours pour les congés
                    if (demandeMiseAJour.getCongeDateDebut() != null && demandeMiseAJour.getCongeDateFin() != null) {
                        long joursPris = ChronoUnit.DAYS.between(
                                demandeMiseAJour.getCongeDateDebut(),
                                demandeMiseAJour.getCongeDateFin()
                        ) + 1; // Inclusif
                        soldeCongeService.debiterSoldeConge(employe, joursPris);
                        soldeCongeService.calculerEtMettreAJourSoldeActuel(employe);
                    }
                } else if (demandeMiseAJour.getCategorie() == CategorieDemande.AUTORISATION) {
                    // Calcul de la durée pour les autorisations
                    LocalTime debut = demandeMiseAJour.getAutoHeureSortieReelle() != null
                            ? demandeMiseAJour.getAutoHeureSortieReelle()
                            : demandeMiseAJour.getAutoHeureDebut();
                    LocalTime fin = demandeMiseAJour.getAutoHeureRetourReel() != null
                            ? demandeMiseAJour.getAutoHeureRetourReel()
                            : demandeMiseAJour.getAutoHeureFin();

                    if (debut != null && fin != null) {
                        long minutes = ChronoUnit.MINUTES.between(debut, fin);
                        // Règle métier : 240 minutes = 0.5 jour
                        double joursPris = (minutes / 240.0) * 0.5;
                        soldeCongeService.debiterSoldeConge(employe, joursPris);
                        soldeCongeService.calculerEtMettreAJourSoldeActuel(employe);
                    }
                }
            }
        } else {
            // Refuser la demande (pas d'impact sur le solde)
            demandeMiseAJour = demandeService.refuserDemande(
                    demandeId, matriculeValidateur, validationRequest.getCommentaire()
            );
        }

        return ResponseEntity.ok(demandeMiseAJour);
    }


    @GetMapping("/demandes-en-attente")
    public ResponseEntity<List<Demande>> getDemandesEnAttenteForChef() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'utilisateur n'est pas authentifié.");
        }

        String matriculeChef = auth.getName();

        if (matriculeChef == null || matriculeChef.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Le matricule du chef n'a pas pu être récupéré.");
        }

        List<Demande> demandes = demandeService.getDemandesEnAttente(matriculeChef);
        return ResponseEntity.ok(demandes);
    }
    // Récupérer l’historique des demandes des subordonnés d’un chef
    @GetMapping("/historique-subordonnes/{matriculeChef}")
    public ResponseEntity<List<Demande>> getHistoriqueSubordonnes(@PathVariable String matriculeChef) {
        List<Demande> demandes = demandeService.getHistoriqueSubordonnes(matriculeChef);
        return ResponseEntity.ok(demandes);
    }

    @GetMapping("/chef")
    public List<DemandeListDTO> listForChef() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String matriculeChef = auth.getName(); // doit être le matricule
        return demandeService.findAllForChef(matriculeChef);
    }

    /** Affichage DRH : demandes des CHEFs (quel que soit le validateur connecté) */
    @GetMapping("/drh")
    public List<DemandeListDTO> listForDrh() {
        return demandeService.findAllForDrh();
    }

    /** Détail commun */
    @GetMapping("/{id}")
    public DemandeDetailDTO detail(@PathVariable Long id) {
        return demandeService.findDetail(id);
    }

    @PostMapping("/{demandeId}/valider")
    public ResponseEntity<Demande> valider(@PathVariable Long demandeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Attempting to validate demandeId " + demandeId + " by user " + matricule);
            Demande updated = demandeService.validerDemande(demandeId, matricule);
            Employe employe = updated.getEmploye();
            if (employe != null) {
                if (updated.getCategorie() == CategorieDemande.CONGE_STANDARD ||
                        updated.getCategorie() == CategorieDemande.CONGE_EXCEPTIONNEL) {
                    if (updated.getCongeDateDebut() != null && updated.getCongeDateFin() != null) {
                        long joursPris = ChronoUnit.DAYS.between(
                                updated.getCongeDateDebut(),
                                updated.getCongeDateFin()
                        ) + 1;
                        logger.info("Debiting " + joursPris + " days for conge, employe: " + employe.getMatricule());
                        soldeCongeService.debiterSoldeConge(employe, joursPris);
                        soldeCongeService.calculerEtMettreAJourSoldeActuel(employe);
                    } else {
                        logger.warning("Conge dates are null for demandeId: " + demandeId);
                    }
                } else if (updated.getCategorie() == CategorieDemande.AUTORISATION) {
                    LocalTime debut = updated.getAutoHeureSortieReelle() != null
                            ? updated.getAutoHeureSortieReelle()
                            : updated.getAutoHeureDebut();
                    LocalTime fin = updated.getAutoHeureRetourReel() != null
                            ? updated.getAutoHeureRetourReel()
                            : updated.getAutoHeureFin();
                    if (debut != null && fin != null) {
                        long minutes = ChronoUnit.MINUTES.between(debut, fin);
                        double joursPris = (minutes / 240.0) * 0.5;
                        logger.info("Debiting " + joursPris + " days for autorisation, employe: " + employe.getMatricule());
                        soldeCongeService.debiterSoldeConge(employe, joursPris);
                        soldeCongeService.calculerEtMettreAJourSoldeActuel(employe);
                    } else {
                        logger.warning("Authorization times are null for demandeId: " + demandeId);
                    }
                }
            } else {
                logger.warning("Employe is null for demandeId: " + demandeId);
            }
            logger.info("Successfully validated demandeId: " + demandeId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.severe("Error validating demandeId " + demandeId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/{demandeId}/refuser")
    public ResponseEntity<Demande> refuser(@PathVariable Long demandeId, @Valid @RequestBody RefusRequest body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Attempting to refuse demandeId " + demandeId + " by user " + matricule);
            Demande updated = demandeService.refuserDemande(demandeId, matricule, body.getCommentaire());
            logger.info("Successfully refused demandeId: " + demandeId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.severe("Error refusing demandeId " + demandeId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


    @GetMapping("get-all-autorisation")
    public ResponseEntity<?>  Autorisation() {
        try {

            List<Demande> demande = demandeService.findByTypeDemande(TypeDemande.AUTORISATION_RETARD);
            demande.addAll(demandeService.findByTypeDemande(TypeDemande.AUTORISATION_SORTIE_PONCTUELLE));
            demande.addAll(demandeService.findByTypeDemande(TypeDemande.AUTORISATION_ABSENCE_EXCEPTIONNELLE));
            return  ResponseEntity.ok(demande);


        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @GetMapping("get-all")
    public  ResponseEntity<?> GetAll(){
        try{
            return  ResponseEntity.ok(demandeService.getAll());
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par statut : " + e.getMessage());
        }
    }


    @GetMapping("get-all-v-r")
    public ResponseEntity<?> GetAllVR() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }

        String matricule = auth.getName();

        try {
            Employe employe = employeService.getEmployeByMatricule(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé introuvable"));

            List<Demande> demandes = demandeService.getDemandesValideesEtRefuseesDuService(
                    employe.getService()
            );

            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des demandes validées/refusées : " + e.getMessage());
        }
    }


    // **************
// Dashboard  *******************************************
    @GetMapping("/count/statut")
    public ResponseEntity<?> countByStatut() {
        try {
            // Retourne une liste d'objets [statut, count]
            return ResponseEntity.ok(demandeService.countByStatutGrouped());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par statut : " + e.getMessage());
        }
    }

    @GetMapping("/count/categorie")
    public ResponseEntity<?> countByCategorie() {
        try {
            // Retourne une liste d'objets [categorie, count]
            return ResponseEntity.ok(demandeService.countDemandesByCategorie());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par catégorie : " + e.getMessage());
        }
    }

    @GetMapping("/count/employe")
    public ResponseEntity<?> countByEmploye() {
        try {
            return ResponseEntity.ok(demandeService.countDemandesByEmploye());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par employé : " + e.getMessage());
        }
    }

    @GetMapping("/count/service")
    public ResponseEntity<?> countByService() {
        try {
            return ResponseEntity.ok(demandeService.countDemandesByService());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par service : " + e.getMessage());
        }
    }

    @GetMapping("/average-validation-time")
    public ResponseEntity<?> getAverageValidationTime() {
        try {
            return ResponseEntity.ok(demandeService.averageValidationTimeSeconds());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du calcul du temps moyen : " + e.getMessage());
        }
    }
    // -------------------- Chart: Count demandes per month (time series) --------------------
    @GetMapping("/chart/month")
    public ResponseEntity<?> countDemandesPerMonth(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr
    ) {
        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);

            // Retourne une liste d'objets [mois, nombre de demandes]
            List<Object[]> result = demandeService.countDemandesPerMonth(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Gestion des erreurs avec message clair
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des demandes par mois : " + e.getMessage());
        }
    }

    // -------------------- Chart: Count demandes per month and year --------------------
    @GetMapping("/chart/month-year")
    public ResponseEntity<?> countDemandesPerMonthAndYear() {
        try {
            // Retourne une liste d'objets [année, mois, nombre de demandes]
            List<Object[]> result = demandeService.countDemandesPerMonthAndYear();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des demandes par mois et année : " + e.getMessage());
        }
    }

    // -------------------- Chart: Count demandes by category per month --------------------
    @GetMapping("/chart/category-month")
    public ResponseEntity<?> countDemandesByCategoriePerMonth(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr
    ) {
        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);

            // Retourne une liste d'objets [categorie, mois, nombre de demandes]
            List<Object[]> result = demandeService.countDemandesByCategoriePerMonth(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des demandes par catégorie par mois : " + e.getMessage());
        }
    }

    // -------------------- Chart: Count demandes by type --------------------
    @GetMapping("/chart/type")
    public ResponseEntity<?> countDemandesByType() {
        try {
            // Retourne une liste d'objets [typeDemande, nombre de demandes]
            List<Object[]> result = demandeService.countDemandesByType();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des demandes par type : " + e.getMessage());
        }
    }

    // -------------------- Filter: Find demandes by employee and status --------------------
    @GetMapping("/filter/employe-statut")
    public ResponseEntity<?> findByEmployeAndStatut(
            @RequestParam("matricule") String matricule,
            @RequestParam("statut") StatutDemande statut
    ) {
        try {
            List<Demande> demandes = demandeService.findByEmployeAndStatut(matricule, statut);
            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la recherche des demandes par employé et statut : " + e.getMessage());
        }
    }

    // -------------------- Filter: Find demandes by type --------------------
    @GetMapping("/filter/type")
    public ResponseEntity<?> findByTypeDemande(
            @RequestParam("type") TypeDemande typeDemande
    ) {
        try {
            List<Demande> demandes = demandeService.findByTypeDemande(typeDemande);
            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la recherche des demandes par type : " + e.getMessage());
        }
    }

    // -------------------- Filter: Find demandes by date range --------------------
    @GetMapping("/filter/date-range")
    public ResponseEntity<?> findByDateCreationBetween(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr
    ) {
        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);

            List<Demande> demandes = demandeService.findByDateCreationBetween(start, end);
            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la recherche des demandes par date : " + e.getMessage());
        }
    }

    // -------------------- Filter: Count demandes by employee in date range --------------------
    @GetMapping("/filter/count-employe-date")
    public ResponseEntity<?> countByEmployeAndDateRange(
            @RequestParam("matricule") String matricule,
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr
    ) {
        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);

            long count = demandeService.countByEmployeAndDateRange(matricule, start, end);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des demandes par employé et date : " + e.getMessage());
        }
    }


    //----------------------------------demades et solde conge for my account
    @GetMapping("get-employe-solde")
    public ResponseEntity<EmployeSoldeDto> getEmployeSolde() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }

        String matricule = auth.getName();

        try {
            // Récupérer l'employé
            Employe emp = employeService.getEmployeByMatricule(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé introuvable"));

            // Récupérer le solde
            SoldeConge soldeConge = soldeCongeService.getSoldeActuel(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solde introuvable"));

            // Mapper vers le DTO
            EmployeSoldeDto dto = new EmployeSoldeDto();
            dto.setNom(emp.getNom());
            dto.setPrenom(emp.getPrenom());
            dto.setGrade(emp.getGrade());
            dto.setService(emp.getService());

            dto.setAnnee(soldeConge.getAnnee());
            dto.setSoldeAu2012(soldeConge.getSoldeAu2012());
            dto.setDroitAnnuel(soldeConge.getDroitAnnuel());
            dto.setDroitN(soldeConge.getDroitN());
            dto.setCongesAcquisN(soldeConge.getCongesAcquisN());
            dto.setRetardsN(soldeConge.getRetardsN());
            dto.setAutorisationsN(soldeConge.getAutorisationsN());
            dto.setSoldeActuel(soldeConge.getSoldeActuel());
            dto.setChefHierarchique1Matricule(emp.getChefHierarchique1Matricule());
            dto.setChefHierarchique2Matricule(emp.getChefHierarchique2Matricule());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des données", e);
        }
    }



    @GetMapping("/historique")
    public ResponseEntity<List<Demande>> getHistoriqueDemandes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Ajout d'une vérification pour déboguer le problème d'authentification
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'utilisateur n'est pas authentifié.");
        }

        String matriculeEmploye = auth.getName();

        if (matriculeEmploye == null || matriculeEmploye.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Le matricule de l'employé n'a pas pu être récupéré.");
        }
        try {


            List<Demande> demandes = demandeService.getHistoriqueDemandes(matriculeEmploye);
            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des données", e);

        }

    }

    @DeleteMapping("cancel-demande/{demandeId}")
    public  ResponseEntity<?> cancelDemande(@PathVariable long demandeId) {
        try{
           Demande demande =  demandeService.getById(demandeId);
           if(demande.getStatut() != StatutDemande.EN_COURS)
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Demande introuvable");
           demandeService.delete(demande);
            return ResponseEntity.ok(true);
        }
        catch (Exception e) {
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    //----------------------------------------------------------------------------

}