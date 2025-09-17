package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.SoldeCongeRepository;
import com.mercedes.workflowrh.service.SoldeCongeService;
import com.mercedes.workflowrh.util.SoldeCongeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class SoldeCongeServiceImpl implements SoldeCongeService {

    @Autowired
    private SoldeCongeRepository soldeCongeRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Override
    public SoldeConge calculerEtMettreAJourSoldeActuel(Employe employe) {
        SoldeConge soldeConge = soldeCongeRepository.findByEmploye(employe)
                .orElse(new SoldeConge());
        soldeConge.setEmploye(employe);
// Calcul du droit annuel en fonction du grade et du matricule
        float droitAnnuel = SoldeCongeCalculator.calculerDroitAnnuel(
                employe.getGrade(),           // grade est Integer donc pas de String.valueOf
                employe.getTypeContrat().name(),     // c’est un String dans ta classe, donc OK direct
                employe.getDateEmbauche()     // LocalDate, OK
        );


        soldeConge.setDroitAnnuel(droitAnnuel);


// Calcul du droit N (droit annuel proportionnel au nombre de mois écoulés)
        float droitN = (droitAnnuel / 12) * LocalDate.now().getMonthValue();
        soldeConge.setDroitN(droitN);

// Initialisation des autres champs
        soldeConge.setCongesAcquisN(0f);
        soldeConge.setRetardsN(0f);
        soldeConge.setAutorisationsN(0f);


        // Calcul du solde actuel
        float soldeActuel = soldeConge.getSoldeAu2012() +
                soldeConge.getDroitN() -
                soldeConge.getCongesAcquisN() -
                soldeConge.getRetardsN() -
                soldeConge.getAutorisationsN();

        soldeConge.setSoldeActuel(soldeActuel);

        return soldeCongeRepository.save(soldeConge);
    }

    @Override
    public Optional<SoldeConge> getSoldeActuel(String matriculeEmploye) {
        return employeRepository.findByMatricule(matriculeEmploye)
                .flatMap(soldeCongeRepository::findByEmploye);
    }

    @Override
    public void debiterSoldeConge(Employe employe, double jours) {
        SoldeConge soldeConge = soldeCongeRepository.findByEmploye(employe)
                .orElseThrow(() -> new RuntimeException("Solde de congé non trouvé pour l'employé"));
        soldeConge.setSoldeActuel(soldeConge.getSoldeActuel() - (float) jours);
        soldeCongeRepository.save(soldeConge);
    }

    @Override
    public void crediterSoldeConge(Employe employe, double jours) {
        SoldeConge soldeConge = soldeCongeRepository.findByEmploye(employe)
                .orElseThrow(() -> new RuntimeException("Solde de congé non trouvé pour l'employé"));
        soldeConge.setSoldeActuel(soldeConge.getSoldeActuel() + (float) jours);
        soldeCongeRepository.save(soldeConge);
    }
}
