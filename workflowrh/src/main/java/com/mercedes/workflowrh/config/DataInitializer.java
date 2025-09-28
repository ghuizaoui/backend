package com.mercedes.workflowrh.config;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.entity.TypeContrat;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.service.SoldeCongeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SoldeCongeService soldeCongeService;

    @Override
    public void run(String... args) {
        // On vérifie si un DRH existe déjà
        if (employeRepository.findByMatricule("DRH001").isEmpty()) {
            Employe superDrh = Employe.builder()
                    .matricule("DRH001")
                    .nom("Admin")
                    .prenom("DRH")
                    .email("drh@entreprise.com")
                    .motDePasse(passwordEncoder.encode("drh@2024")) // Mot de passe à changer à la 1ère connexion
                    .role(Role.DRH)
                    .drhSuper(false)
                    .premiereConnexion(false)
                    .build();
            employeRepository.save(superDrh);
            System.out.println("==== DRH initial créé automatiquement ====");
        }

        if (employeRepository.findByMatricule("DG001").isEmpty()) {
            Employe dg = Employe.builder()
                    .matricule("DG001")
                    .nom("Admin")
                    .prenom("DG")
                    .email("dg@entreprise.com")
                    .motDePasse(passwordEncoder.encode("dg@2024")) // Mot de passe temporaire
                    .role(Role.DRH)  // ⚡ tu dois avoir un enum Role.DG
                    .direction("Direction Générale")
                    .service("Direction Générale")
                    .grade(1) // par exemple le plus haut grade
                    .dateEmbauche(LocalDate.now())
                    .typeContrat(TypeContrat.CDI) // choix par défaut
                    .premiereConnexion(false)     // déjà activé
                    .drhSuper(true)              // pas DRH
                    .chefLevel(0)
                    .estBanni(false)
                    .build();

            employeRepository.save(dg);
            soldeCongeService.calculerEtMettreAJourSoldeActuel(dg);
            System.out.println("==== DG initial créé automatiquement ====");
        }

    }
}
