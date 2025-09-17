// Dans le package `com.mercedes.workflowrh.controller`
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.service.SoldeCongeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/soldes-conges")
@CrossOrigin(origins = "http://localhost:4200") // Permet les requêtes de votre front-end Angular
public class SoldeCongeController {

    private final SoldeCongeService soldeCongeService;

    @Autowired
    public SoldeCongeController(SoldeCongeService soldeCongeService) {
        this.soldeCongeService = soldeCongeService;
    }

    @GetMapping("/solde/{matriculeEmploye}")
    public ResponseEntity<SoldeConge> getSoldeActuelByMatricule(@PathVariable String matriculeEmploye) {
        return soldeCongeService.getSoldeActuel(matriculeEmploye)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}