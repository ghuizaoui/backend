// DashboardChefController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.dashboardDto.DashboardChefDTO;
import com.mercedes.workflowrh.service.DashboardChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard/chef")
@RequiredArgsConstructor
public class DashboardChefController {

    private final DashboardChefService dashboardChefService;

    @GetMapping
    public ResponseEntity<DashboardChefDTO> getDashboardChef(
            @RequestParam(value = "dateDebut", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") String dateDebut,
            @RequestParam(value = "dateFin", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") String dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {


            // Définir les dates par défaut si non fournies (année en cours)
            String startDate = dateDebut != null ? dateDebut : LocalDateTime.now().getYear() + "-01-01";
            String endDate = dateFin != null ? dateFin : LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/periode")
    public ResponseEntity<DashboardChefDTO> getDashboardChefWithPeriod(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, dateDebut, dateFin);
            return ResponseEntity.ok(dashboard);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/vue-ensemble")

    public ResponseEntity<?> getVueEnsembleDemandes(

            @RequestParam(value = "dateDebut", required = false) String dateDebut,
            @RequestParam(value = "dateFin", required = false) String dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {

            String startDate = dateDebut != null ? dateDebut : LocalDateTime.now().getYear() + "-01-01";
            String endDate = dateFin != null ? dateFin : LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard.getVueEnsembleDemandes());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la récupération de la vue d'ensemble: " + e.getMessage());
        }
    }

    @GetMapping("/repartition-statuts")
    public ResponseEntity<?> getRepartitionStatuts(

            @RequestParam(value = "dateDebut", required = false) String dateDebut,
            @RequestParam(value = "dateFin", required = false) String dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {

            String startDate = dateDebut != null ? dateDebut : LocalDateTime.now().getYear() + "-01-01";
            String endDate = dateFin != null ? dateFin : LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard.getRepartitionStatuts());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la récupération de la répartition des statuts: " + e.getMessage());
        }
    }

    @GetMapping("/conges")
    public ResponseEntity<?> getJoursCongesPris(

            @RequestParam(value = "dateDebut", required = false) String dateDebut,
            @RequestParam(value = "dateFin", required = false) String dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {

            String startDate = dateDebut != null ? dateDebut : LocalDateTime.now().getYear() + "-01-01";
            String endDate = dateFin != null ? dateFin : LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard.getJoursCongesPris());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la récupération des données de congés: " + e.getMessage());
        }
    }

    @GetMapping("/demandes-acceptees")
    public ResponseEntity<?> getDemandesAcceptees(

            @RequestParam(value = "dateDebut", required = false) String dateDebut,
            @RequestParam(value = "dateFin", required = false) String dateFin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();

        try {

            String startDate = dateDebut != null ? dateDebut : LocalDateTime.now().getYear() + "-01-01";
            String endDate = dateFin != null ? dateFin : LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard.getDemandesAccepteesServices());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la récupération des demandes acceptées: " + e.getMessage());
        }
    }

    @GetMapping("/soldes-employes")
    public ResponseEntity<?> getSoldesEmployes() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {

            // Pour les soldes, on n'a pas besoin de période, donc on utilise l'année en cours par défaut
            String startDate = LocalDateTime.now().getYear() + "-01-01";
            String endDate = LocalDateTime.now().getYear() + "-12-31";

            DashboardChefDTO dashboard = dashboardChefService.getDashboardChef(matricule, startDate, endDate);
            return ResponseEntity.ok(dashboard.getSoldesEmployes());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la récupération des soldes des employés: " + e.getMessage());
        }
    }
}