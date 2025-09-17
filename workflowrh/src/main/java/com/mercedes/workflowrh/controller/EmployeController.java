package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.service.EmployeService;
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

    @PostMapping("/add")
    public ResponseEntity<Employe> ajouterEmploye(@RequestBody EmployeDTO dto) {
        System.out.println("SECURITY DEBUG : " + SecurityContextHolder.getContext().getAuthentication());
        Employe employe = employeService.ajouterEmploye(dto);
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
