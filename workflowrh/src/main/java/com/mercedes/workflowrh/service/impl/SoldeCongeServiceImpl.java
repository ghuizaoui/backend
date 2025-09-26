package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.entity.CategorieDemande;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.SoldeCongeRepository;
import com.mercedes.workflowrh.service.SoldeCongeService;
import com.mercedes.workflowrh.util.SoldeCongeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SoldeCongeServiceImpl implements SoldeCongeService {

    @Autowired
    private SoldeCongeRepository soldeCongeRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Override
    @Transactional
    public SoldeConge calculerEtMettreAJourSoldeActuel(Employe employe) {
        List<SoldeConge> existingRecords = soldeCongeRepository.findAllByEmploye(employe);
        SoldeConge soldeConge;

        if (existingRecords.size() > 1) {
            System.err.println("Warning: Multiple SoldeConge records found for employee: " + employe.getMatricule());
            soldeConge = selectMostRecentSolde(existingRecords);
            deleteDuplicateSoldeRecords(existingRecords, soldeConge);
        } else if (existingRecords.size() == 1) {
            soldeConge = existingRecords.get(0);
        } else {
            soldeConge = createNewSoldeConge(employe);
        }

        soldeConge.setEmploye(employe);
        float droitAnnuel = SoldeCongeCalculator.calculerDroitAnnuel(
                employe.getGrade(),
                employe.getTypeContrat().name(),
                employe.getDateEmbauche()
        );
        soldeConge.setDroitAnnuel(droitAnnuel);
        float droitN = (droitAnnuel / 12) * LocalDate.now().getMonthValue();
        soldeConge.setDroitN(droitN);

        float soldeActuel = soldeConge.getSoldeAu2012() +
                soldeConge.getDroitN() -
                soldeConge.getCongesAcquisN() -
                soldeConge.getRetardsN() -
                soldeConge.getAutorisationsN();
        soldeConge.setSoldeActuel(soldeActuel);

        return soldeCongeRepository.save(soldeConge);
    }

    @Override
    @Transactional
    public void debiterSoldeConge(Employe employe, double jours) {
        debiterSoldeConge(employe, jours, null);
    }

    @Override
    @Transactional
    public void debiterSoldeConge(Employe employe, double jours, CategorieDemande categorie) {
        try {
            List<SoldeConge> soldes = soldeCongeRepository.findAllByEmployeWithLock(employe);
            SoldeConge solde;

            if (soldes.size() > 1) {
                System.err.println("Warning: Multiple SoldeConge records found for employee: " + employe.getMatricule());
                solde = selectMostRecentSolde(soldes);
                deleteDuplicateSoldeRecords(soldes, solde);
            } else if (soldes.size() == 1) {
                solde = soldes.get(0);
            } else {
                throw new RuntimeException("Solde non trouvé pour l'employé: " + employe.getMatricule());
            }

            System.out.println("=== DÉBIT SOLDE ===");
            System.out.println("Avant débit - Solde actuel: " + solde.getSoldeActuel());
            System.out.println("Jours à débiter: " + jours);
            System.out.println("Employé: " + employe.getMatricule());
            System.out.println("Catégorie: " + (categorie != null ? categorie : "N/A"));

            if (solde.getSoldeActuel() < jours) {
                throw new RuntimeException("Solde insuffisant. Solde actuel: " +
                        solde.getSoldeActuel() + ", requis: " + jours);
            }

            float nouveauSolde = solde.getSoldeActuel() - (float) jours;
            solde.setSoldeActuel(nouveauSolde);

            if (categorie != null) {
                if (categorie == CategorieDemande.CONGE_STANDARD || categorie == CategorieDemande.CONGE_EXCEPTIONNEL) {
                    solde.setCongesAcquisN(solde.getCongesAcquisN() + (float) jours);
                } else if (categorie == CategorieDemande.AUTORISATION) {
                    solde.setAutorisationsN(solde.getAutorisationsN() + (float) jours);
                }
            }

            SoldeConge soldeSauvegarde = soldeCongeRepository.save(solde);
            System.out.println("Après débit - Nouveau solde: " + soldeSauvegarde.getSoldeActuel());
            System.out.println("=== FIN DÉBIT ===");
        } catch (Exception e) {
            System.err.println("Erreur lors du débit du solde pour employé " + employe.getMatricule() + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean estSoldeNegatif(Employe employe) {
        Optional<SoldeConge> soldeConge = getSoldeForEmployee(employe);
        return soldeConge.map(solde -> solde.getSoldeActuel() < 0).orElse(true);
    }

    @Override
    public void verrouillerSiSoldeNegatif(Employe employe) {
        if (estSoldeNegatif(employe)) {
            throw new RuntimeException("Le solde est négatif, nouvelles demandes bloquées.");
        }
    }

    @Override
    public float calculerDroitMensuel(Employe employe) {
        float droitAnnuel = SoldeCongeCalculator.calculerDroitAnnuel(
                employe.getGrade(),
                employe.getTypeContrat().name(),
                employe.getDateEmbauche()
        );
        return droitAnnuel / 12;
    }

    @Override
    @Transactional
    public void reinitialiserSoldeAnnuel(Employe employe, int nouvelleAnnee) {
        List<SoldeConge> existingRecords = soldeCongeRepository.findAllByEmploye(employe);
        float previousSoldeActuel = existingRecords.isEmpty() ? 0f : selectMostRecentSolde(existingRecords).getSoldeActuel();

        existingRecords.forEach(soldeCongeRepository::delete);

        SoldeConge nouveauSolde = SoldeConge.builder()
                .employe(employe)
                .annee(nouvelleAnnee)
                .soldeAu2012(previousSoldeActuel)
                .droitAnnuel(SoldeCongeCalculator.calculerDroitAnnuel(
                        employe.getGrade(),
                        employe.getTypeContrat().name(),
                        employe.getDateEmbauche()))
                .droitN(0f)
                .congesAcquisN(0f)
                .retardsN(0f)
                .autorisationsN(0f)
                .soldeActuel(previousSoldeActuel)
                .build();

        soldeCongeRepository.save(nouveauSolde);
    }

    @Override
    @Transactional
    public SoldeConge creerSoldeConge(Employe employe) {
        List<SoldeConge> existingRecords = soldeCongeRepository.findAllByEmploye(employe);
        if (!existingRecords.isEmpty()) {
            System.err.println("SoldeConge already exists for employee: " + employe.getMatricule());
            return selectMostRecentSolde(existingRecords);
        }

        SoldeConge solde = new SoldeConge();
        solde.setEmploye(employe);
        solde.setSoldeAu2012(0f);
        solde.setDroitAnnuel(0f);
        solde.setDroitN(0f);
        solde.setCongesAcquisN(0f);
        solde.setRetardsN(0f);
        solde.setAutorisationsN(0f);
        solde.setSoldeActuel(0f);
        solde.setAnnee(LocalDate.now().getYear());
        return soldeCongeRepository.save(solde);
    }

    @Override
    public Optional<SoldeConge> getSoldeActuel(String matriculeEmploye) {
        return employeRepository.findByMatricule(matriculeEmploye)
                .flatMap(this::getSoldeForEmployee);
    }

    @Override
    @Transactional
    public void crediterSoldeConge(Employe employe, double jours) {
        Optional<SoldeConge> soldeCongeOpt = getSoldeForEmployee(employe);
        SoldeConge soldeConge = soldeCongeOpt.orElseThrow(() -> new RuntimeException("Solde de congé non trouvé pour l'employé: " + employe.getMatricule()));
        float nouveauSolde = soldeConge.getSoldeActuel() + (float) jours;
        soldeConge.setSoldeActuel(nouveauSolde);
        soldeCongeRepository.save(soldeConge);
    }

    private Optional<SoldeConge> getSoldeForEmployee(Employe employe) {
        List<SoldeConge> soldes = soldeCongeRepository.findAllByEmploye(employe);
        if (soldes.size() > 1) {
            System.err.println("Multiple SoldeConge records found for employee: " + employe.getMatricule());
            return Optional.of(selectMostRecentSolde(soldes));
        }
        return soldes.stream().findFirst();
    }

    private SoldeConge selectMostRecentSolde(List<SoldeConge> soldes) {
        return soldes.stream()
                .max((s1, s2) -> Integer.compare(s1.getAnnee(), s2.getAnnee()))
                .orElseThrow(() -> new RuntimeException("No valid SoldeConge record found after filtering"));
    }

    private void deleteDuplicateSoldeRecords(List<SoldeConge> soldes, SoldeConge keptSolde) {
        soldes.stream()
                .filter(sc -> !sc.getId().equals(keptSolde.getId()))
                .forEach(sc -> {
                    soldeCongeRepository.delete(sc);
                    System.out.println("Deleted duplicate SoldeConge record ID: " + sc.getId() + " for employee: " + sc.getEmploye().getMatricule());
                });
    }

    private SoldeConge createNewSoldeConge(Employe employe) {
        SoldeConge solde = new SoldeConge();
        solde.setEmploye(employe);
        solde.setSoldeAu2012(0f);
        solde.setCongesAcquisN(0f);
        solde.setRetardsN(0f);
        solde.setAutorisationsN(0f);
        solde.setAnnee(LocalDate.now().getYear());
        return solde;
    }
}