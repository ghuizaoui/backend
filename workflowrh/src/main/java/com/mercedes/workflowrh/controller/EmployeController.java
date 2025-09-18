package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
}
