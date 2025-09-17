// src/main/java/com/mercedes/workflowrh/controller/DemandeController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.*;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.service.DemandeService;
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
import java.util.List;


@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    @PostMapping("/conge-standard")
    public ResponseEntity<Demande> createCongeStandard(@Valid @RequestBody CongeRequest req) {
        return ResponseEntity.ok(
                demandeService.createCongeStandard(
                        req.getTypeDemande(),
                        req.getDateDebut(), req.getHeureDebut(),
                        req.getDateFin(),   req.getHeureFin()
                )
        );
    }

    @PostMapping("/conge-exceptionnel")
    public ResponseEntity<Demande> createCongeExceptionnel(@Valid @RequestBody CongeRequest req) {
        return ResponseEntity.ok(
                demandeService.createCongeExceptionnel(
                        req.getTypeDemande(),
                        req.getDateDebut(), req.getHeureDebut(),
                        req.getDateFin(),   req.getHeureFin()
                )
        );
    }

    // src/main/java/com/mercedes/workflowrh/controller/DemandeController.java
    @PostMapping("/autorisation")
    public ResponseEntity<Demande> createAutorisation(@Valid @RequestBody AutorisationRequest req) {
        return ResponseEntity.ok(
                demandeService.createAutorisation(
                        req.getTypeDemande(),

                        // PRÉVU (requis)
                        req.getDateAutorisation(),
                        req.getHeureDebut(),
                        req.getHeureFin(),

                        // RÉEL (optionnel)
                        req.getDateReelle(),
                        req.getHeureSortieReelle(),
                        req.getHeureRetourReel()
                )
        );
    }


    @PostMapping("/ordre-mission")
    public ResponseEntity<Demande> createOrdreMission(@Valid @RequestBody OrdreMissionRequest req) {
        return ResponseEntity.ok(
                demandeService.createOrdreMission(
                        req.getDateDebut(), req.getHeureDebut(),
                        req.getDateFin(),   req.getHeureFin(),
                        req.getMissionObjet()
                )
        );
    }




    @PostMapping("/validation/{demandeId}")
    public ResponseEntity<Demande> validerRefuserDemande(
            @PathVariable Long demandeId,
            @RequestBody ValidationRequest validationRequest) { // Utilise le DTO

        // Récupérer l'employé connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String matriculeValidateur = auth.getName();

        // Appeler le service pour traiter la validation ou le refus
        Demande demandeMiseAJour;
        if (validationRequest.getIsValidee()) {
            demandeMiseAJour = demandeService.validerDemande(demandeId, matriculeValidateur);
        } else {
            demandeMiseAJour = demandeService.refuserDemande(demandeId, matriculeValidateur, validationRequest.getCommentaire());
        }

        return ResponseEntity.ok(demandeMiseAJour);
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

        List<Demande> demandes = demandeService.getHistoriqueDemandes(matriculeEmploye);
        return ResponseEntity.ok(demandes);
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
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String matricule = auth.getName();
        Demande updated = demandeService.validerDemande(demandeId, matricule);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{demandeId}/refuser")
    public ResponseEntity<Demande> refuser(@PathVariable Long demandeId, @Valid @RequestBody RefusRequest body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String matricule = auth.getName();
        Demande updated = demandeService.refuserDemande(demandeId, matricule, body.getCommentaire());
        return ResponseEntity.ok(updated);
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
}