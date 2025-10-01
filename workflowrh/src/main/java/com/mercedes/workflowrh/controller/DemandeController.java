// src/main/java/com/mercedes/workflowrh/controller/DemandeController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.*;
import com.mercedes.workflowrh.dto.dashboardDto.CategoryTypeDistributionDTO;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.DemandeRepository;
import com.mercedes.workflowrh.service.DemandeService;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import com.mercedes.workflowrh.service.impl.HolidayService;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mercedes.workflowrh.security.AppUserDetailsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;


@Slf4j
@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService  demandeService;
    private  final SoldeCongeService soldeCongeService;
    private static final Logger logger = Logger.getLogger(DemandeController.class.getName());
    private final EmployeService employeService;
    private  final DemandeRepository demandeRepository;
    private  final HolidayService holidayService;
    private  final MailService mailService ;


    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée avec l'ID: " + id));

        if (demande.getFile() == null || demande.getFile().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun fichier associé à cette demande.");
        }

        String contentType = "application/pdf";
        String fileName = "demande_" + id + ".pdf";
        ByteArrayResource resource = new ByteArrayResource(demande.getFile());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping("/conge-standard")
    public ResponseEntity<Demande> createCongeStandard(@Valid @RequestBody CongeRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Creating CONGE_STANDARD demand for user: " + matricule);

            long joursOuvres = holidayService.calculateWorkingDays(req.getDateDebut(), req.getDateFin());
            Employe employe = demandeService.getEmployeByMatricule(matricule);
            float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solde non trouvé"))
                    .getSoldeActuel();

            if (soldeActuel < joursOuvres) {
                logger.warning("Solde insuffisant pour CONGE_STANDARD: requis " + joursOuvres + ", disponible " + soldeActuel);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solde insuffisant: requis " + joursOuvres + ", disponible " + soldeActuel);
            }

            if (joursOuvres == 0) {
                logger.warning("Aucun jour ouvré dans la période sélectionnée");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun jour ouvré dans la période sélectionnée.");
            }

            Demande demande = demandeService.createCongeStandard(
                    req.getTypeDemande(),
                    req.getDateDebut(), req.getHeureDebut(),
                    req.getDateFin(), req.getHeureFin()
            );
            logger.info("CONGE_STANDARD créé avec ID: " + demande.getId() + " - Jours ouvrés: " + joursOuvres);
            return ResponseEntity.ok(demande);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Erreur création CONGE_STANDARD: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/conge-exceptionnel", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Demande> createCongeExceptionnel(
            @RequestPart("request") @Valid CongeExceptionnelRequest req,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Creating CONGE_EXCEPTIONNEL demand for user: " + matricule +
                    ", typeDemande: " + req.getTypeDemande() +
                    ", dateDebut: " + req.getDateDebut() +
                    ", heureDebut: " + req.getHeureDebut() +
                    ", dateFin: " + req.getDateFin() +
                    ", heureFin: " + req.getHeureFin() +
                    ", file: " + (file != null ? file.getOriginalFilename() : "none"));

            // Check solde if deducts
            if (req.getTypeDemande().deductsFromSolde()) {
                long joursOuvres = holidayService.calculateWorkingDays(req.getDateDebut(), req.getDateFin());
                Employe employe = demandeService.getEmployeByMatricule(matricule);
                float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solde non trouvé"))
                        .getSoldeActuel();
                if (soldeActuel < joursOuvres) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solde insuffisant: requis " + joursOuvres + ", disponible " + soldeActuel);
                }
            }

            Demande demande = demandeService.createCongeExceptionnel(
                    req.getTypeDemande(),
                    req.getDateDebut(), req.getHeureDebut(),
                    req.getDateFin(), req.getHeureFin(),req.getInterimaireMatricule(),
                    file
            );
            logger.info("Successfully created CONGE_EXCEPTIONNEL demand with ID: " + demande.getId());
            return ResponseEntity.ok(demande);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error creating CONGE_EXCEPTIONNEL demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/autorisation")
    public ResponseEntity<Demande> createAutorisation(@Valid @RequestBody AutorisationRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warning("Unauthorized access attempt to create AUTORISATION demand.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Creating AUTORISATION demand for user: " + matricule);

            LocalTime debut = req.getHeureSortieReelle() != null ? req.getHeureSortieReelle() : req.getHeureDebut();
            LocalTime fin = req.getHeureRetourReel() != null ? req.getHeureRetourReel() : req.getHeureFin();
            if (debut != null && fin != null) {
                long minutes = ChronoUnit.MINUTES.between(debut, fin);
                if (minutes > 120) {
                    logger.warning("Autorisation duration exceeds 2 hours: " + minutes + " minutes");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La durée de l'autorisation ne doit pas dépasser 2 heures (120 minutes).");
                }
                if (!holidayService.isFreeDay(req.getDateAutorisation())) {
                    double joursPris = (minutes / 240.0) * 0.5;
                    Employe employe = demandeService.getEmployeByMatricule(matricule);
                    float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solde non trouvé"))
                            .getSoldeActuel();
                    if (soldeActuel < joursPris) {
                        logger.warning("Insufficient balance for AUTORISATION: required " + joursPris + ", available " + soldeActuel);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Solde insuffisant pour cette autorisation: requis " + joursPris + ", disponible " + soldeActuel);
                    }
                }
            } else {
                logger.warning("Invalid time fields for AUTORISATION: debut=" + debut + ", fin=" + fin);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les champs heure de début et heure de fin sont requis.");
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
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Unexpected error creating AUTORISATION demand for user " + matricule + ": " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création de l'autorisation.", e);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/validation/{demandeId}")
    public ResponseEntity<Demande> validerRefuserDemande(
            @PathVariable Long demandeId,
            @RequestBody ValidationRequest validationRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matriculeValidateur = auth.getName();

        try {
            Demande demandeMiseAJour;
            if (validationRequest.getIsValidee()) {
                demandeMiseAJour = demandeService.validerDemande(demandeId, matriculeValidateur);
                // Deduction handled in service
            } else {
                demandeMiseAJour = demandeService.refuserDemande(demandeId, matriculeValidateur, validationRequest.getCommentaire());
            }
            return ResponseEntity.ok(demandeMiseAJour);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Error processing demandeId " + demandeId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{demandeId}/valider")
    public ResponseEntity<Demande> valider(@PathVariable Long demandeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            logger.info("Tentative de validation demandeId " + demandeId + " par utilisateur " + matricule);
            Demande updated = demandeService.validerDemande(demandeId, matricule);
            logger.info("DemandeId " + demandeId + " validée avec succès");
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("Erreur validation demandeId " + demandeId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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

    @GetMapping("/historique-subordonnes/{matriculeChef}")
    public ResponseEntity<List<Demande>> getHistoriqueSubordonnes(@PathVariable String matriculeChef) {
        List<Demande> demandes = demandeService.getHistoriqueSubordonnes(matriculeChef);
        return ResponseEntity.ok(demandes);
    }
    @GetMapping("/chef")
    public ResponseEntity<List<DemandeListDTO>> listForChef() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matriculeChef = auth.getName();
        Employe chef = employeService.getEmployeByMatricule(matriculeChef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chef non trouvé"));

        if (chef.getRole() != com.mercedes.workflowrh.entity.Role.CHEF ||
                chef.getChefLevel() == null ||
                (chef.getChefLevel() != 1 && chef.getChefLevel() != 2)) {
            return ResponseEntity.ok(List.of()); // Return empty list for non-chefs or invalid chef levels
        }

        return ResponseEntity.ok(demandeService.findAllForChef(matriculeChef, chef.getChefLevel(), chef.getService()));
    }

    @GetMapping("/drh")
    public List<DemandeListDTO> listForDrh() {
        return demandeService.findAllForDrh();
    }

    @GetMapping("/{id}")
    public DemandeDetailDTO detail(@PathVariable Long id) {
        return demandeService.findDetail(id);
    }

    @GetMapping("/get-all-autorisation-order-mission")
    public ResponseEntity<?> Autorisation() {
        try {
            List<Demande> demande = demandeRepository.findByCategorie(CategorieDemande.AUTORISATION);
            demande.addAll(demandeRepository.findByCategorie(CategorieDemande.ORDRE_MISSION));
            return ResponseEntity.ok(demande);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> GetAll() {
        try {
            return ResponseEntity.ok(demandeService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage par statut : " + e.getMessage());
        }
    }

    @GetMapping("/get-all-v-r")
    public ResponseEntity<?> GetAllVR() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            Employe employe = employeService.getEmployeByMatricule(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé introuvable"));
            List<Demande> demandes = demandeService.getDemandesValideesEtRefuseesDuService(employe.getService());
            return ResponseEntity.ok(demandes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des demandes validées/refusées : " + e.getMessage());
        }
    }

    @GetMapping("/get-employe-solde")
    public ResponseEntity<EmployeSoldeDto> getEmployeSolde() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            Employe emp = employeService.getEmployeByMatricule(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé introuvable"));
            SoldeConge soldeConge = soldeCongeService.getSoldeActuel(matricule)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solde introuvable"));
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

    @DeleteMapping("/cancel-demande/{demandeId}")
    public ResponseEntity<?> cancelDemande(@PathVariable long demandeId) {
        try {
            Demande demande = demandeService.getById(demandeId);
            if (demande.getStatut() != StatutDemande.EN_COURS)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Demande introuvable ou non annulable");
            demandeService.delete(demande);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/calculate-working-days")
    public ResponseEntity<Long> calculateWorkingDays(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            long workingDays = holidayService.calculateWorkingDays(start, end);
            return ResponseEntity.ok(workingDays);
        } catch (Exception e) {
            logger.severe("Erreur calcul jours ouvrés: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(0L);
        }
    }

    @GetMapping("/is-free-day/{date}")
    public ResponseEntity<Boolean> isFreeDay(@PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            boolean isFree = holidayService.isFreeDay(localDate);
            return ResponseEntity.ok(isFree);
        } catch (Exception e) {
            logger.severe("Erreur vérification jour libre: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
    }

    // Dashboard endpoints (unchanged for brevity)
    @GetMapping("/count/statut")
    public ResponseEntity<?> countByStatut() {
        try {
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

    @GetMapping("get-dg-demandes")
    public ResponseEntity<?> getDemandesDG() {
        try{

            List<Demande> d= demandeService.getChefsDemandes();
            if (!d.isEmpty()) {
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + d.get(0).getTypeDemande());
            }

            return  ResponseEntity.ok(demandeService.getChefsDemandes());
        }
        catch(Exception e){
            return  ResponseEntity.status(500).body(e.getMessage());
        }
    }





    @GetMapping("get-demandes-today")
    public  ResponseEntity<?> getDemandesToday() {
        try {


            return ResponseEntity.ok(demandeService.findValidatedDemandesToday());

        }
        catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }



}