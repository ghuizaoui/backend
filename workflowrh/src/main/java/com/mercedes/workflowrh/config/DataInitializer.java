package com.mercedes.workflowrh.config;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                    .premiereConnexion(false)
                    .build();
            employeRepository.save(superDrh);
            System.out.println("==== DRH initial créé automatiquement ====");
        }
    }
}
