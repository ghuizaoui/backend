package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;
    private  final SoldeCongeService soldeCongeService;

    @PostMapping("/add")
    public ResponseEntity<Employe> ajouterEmploye(@RequestBody EmployeDTO dto) {
        System.out.println("SECURITY DEBUG : " + SecurityContextHolder.getContext().getAuthentication());

        // 1. Création de l’employé
        Employe employe = employeService.ajouterEmploye(dto);
        employe.setEstBanni(false);

        // 2. Création automatique du solde de congé
        try {
            SoldeConge soldeConge = soldeCongeService.creerSoldeConge(employe);
            System.out.println("Solde de congé créé : " + soldeConge.getSoldeActuel());
        } catch (Exception e) {
            System.err.println(" Erreur lors de la création du solde de congé : " + e.getMessage());
        }

        return ResponseEntity.ok(employe);
    }

    @PostMapping("/premiere-connexion")
    public ResponseEntity<Void> changerMotDePasse(
            @RequestParam String matricule,
            @RequestParam String nouveauMotDePasse) {
        employeService.changerMotDePassePremiereConnexion(matricule, nouveauMotDePasse);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Employe>> getAllEmployes() {
        return ResponseEntity.ok(employeService.getAllEmployes());
    }

    @GetMapping("/by-matricule/{matricule}")
    public ResponseEntity<Employe> getEmployeByMatricule(@PathVariable String matricule) {
        return employeService.getEmployeByMatricule(matricule)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profile/{matricule}")
    public ResponseEntity<Employe> getEmployeProfile(@PathVariable String matricule) {
        return employeService.getEmployeProfile(matricule)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update/{matricule}")
    public ResponseEntity<Employe> updateEmploye(
            @PathVariable String matricule,
            @RequestBody EmployeDTO dto) {
        try {
            Employe updated = employeService.updateEmploye(matricule, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


        @PutMapping("/banne/{matricule}")
        public ResponseEntity<?> banneEmploye(@PathVariable String matricule) {
            try {
                Employe employe = employeService.getEmployeByMatricule(matricule).get();
                employe.setEstBanni(true);

                employeService.updateEmploye(employe);
                System.out.println("////////////////////////////**************////////"+employe.getEstBanni());
                return  ResponseEntity.status(200).body(true);
            }
            catch (Exception e) {
                return ResponseEntity.status(500).build();
            }
        }


        @PutMapping("/unbanne/{matricule}")
        public ResponseEntity<?> unbanneEmploye(@PathVariable String matricule) {
            try {
                // Récupérer l'employé
                Employe employe = employeService.getEmployeByMatricule(matricule).orElseThrow(
                        () -> new RuntimeException("Employé non trouvé")
                );

                // Définir estBanni à false
                employe.setEstBanni(false);
                employeService.updateEmploye(employe);

                // Retourner succès
                return ResponseEntity.ok(true);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Erreur lors du déblocage de l'employé");
            }
        }


    @GetMapping("/estsuper")
    public  ResponseEntity<?> estSuper() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        String matricule = auth.getName();
        try{
            Employe employe = employeService.getEmployeByMatricule(matricule)
                    .orElseThrow(() -> new RuntimeException("Employé introuvable"));

// Si tu veux vérifier uniquement les DRH
            if (employe.getRole() != Role.DRH) {
                return ResponseEntity.badRequest().body("user n'est pas DRH");
            }

// Vérification safe pour Boolean
            if (!Boolean.TRUE.equals(employe.getDrhSuper())) {
                return ResponseEntity.badRequest().body("user n'est pas super DRH");
            }

            return ResponseEntity.ok(true);

        }
        catch (Exception e){
            return  ResponseEntity.status(500).body(false);
        }

    }



       @GetMapping("/me")
        public  ResponseEntity<?> me() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
            }
            String matricule = auth.getName();
            try{

                return  ResponseEntity.ok(employeService.getEmployeByMatricule(matricule).get());
            } catch (Exception e) {

                return  ResponseEntity.status(500).body(false);
            }

        }
}
