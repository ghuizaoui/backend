package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.dashboardDto.EmployeDashboardDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.service.DemandeService;

import com.mercedes.workflowrh.service.EmployeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employe-dashboard")
@RequiredArgsConstructor
public class EmployeDashboardController {

    private final DemandeService demandeService;
    private  final EmployeService employeService;

    @GetMapping
    public ResponseEntity<EmployeDashboardDTO> getDashboard(
            ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {

            Employe employe = employeService.getEmployeByMatricule(matricule).get();
            if (!(employe.getRole() == Role.EMPLOYE || employe.getRole()== Role.CONCIERGE)) {
                return ResponseEntity.badRequest().build();
            }

            EmployeDashboardDTO dashboard = demandeService.getEmployeDashboard(employe.getMatricule(), employe.getRole().toString());
            return ResponseEntity.ok(dashboard);
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/autorisations-aujourdhui")
    public ResponseEntity<?> getAutorisationsAujourdhui(@RequestParam String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try {
            Employe employe = employeService.getEmployeByMatricule(matricule).get();

            // FIXED: Only concierge can access this endpoint
            if (!Role.CONCIERGE.equals(employe.getRole())) {
                return ResponseEntity.status(403).body("Access denied. Only concierge can view today's authorizations.");
            }

            // Use the correct method for concierge (validated autorisations only)
            return ResponseEntity.ok(demandeService.getValidatedAutorisationsForToday());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}