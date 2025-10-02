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

        // Calculate cumulative balance from ALL previous years
        float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, soldeConge.getAnnee());

        float soldeActuel = cumulSoldePrecedent
                + soldeConge.getDroitAnnuel()
                - soldeConge.getCongesAcquisN()
                - soldeConge.getRetardsN()
                - soldeConge.getAutorisationsN();
        soldeConge.setSoldeActuel(soldeActuel);
        soldeConge.setReport(cumulSoldePrecedent); // Set the report to the cumulative balance

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

        // Calculate cumulative balance from ALL previous years (not just last year)
        float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, nouvelleAnnee);

        // Delete existing records for the new year if any
        existingRecords.stream()
                .filter(solde -> solde.getAnnee() == nouvelleAnnee)
                .forEach(soldeCongeRepository::delete);

        SoldeConge nouveauSolde = SoldeConge.builder()
                .employe(employe)
                .annee(nouvelleAnnee)
                .report(cumulSoldePrecedent) // Report the cumulative balance from ALL previous years
                .droitAnnuel(SoldeCongeCalculator.calculerDroitAnnuel(
                        employe.getGrade(),
                        employe.getTypeContrat().name(),
                        employe.getDateEmbauche()))
                .droitN(0f)
                .congesAcquisN(0f)
                .retardsN(0f)
                .autorisationsN(0f)
                .soldeActuel(cumulSoldePrecedent) // Initial balance = cumulative balance from ALL previous years
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

        // Calculate cumulative balance from ALL previous years if any
        float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, LocalDate.now().getYear());

        SoldeConge solde = new SoldeConge();
        solde.setEmploye(employe);
        solde.setSoldeAu2012(0f);
        solde.setDroitAnnuel(0f);
        solde.setDroitN(0f);
        solde.setCongesAcquisN(0f);
        solde.setRetardsN(0f);
        solde.setAutorisationsN(0f);
        solde.setSoldeActuel(cumulSoldePrecedent); // Set initial balance to cumulative from ALL years
        solde.setReport(cumulSoldePrecedent); // Set report to cumulative balance from ALL years
        solde.setAnnee(LocalDate.now().getYear());
        return soldeCongeRepository.save(solde);
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
    @Override
    public Optional<SoldeConge> getSoldeActuel(String matriculeEmploye) {
        return employeRepository.findByMatricule(matriculeEmploye)
                .flatMap(employe -> {
                    Optional<SoldeConge> soldeOpt = getSoldeForEmployee(employe);

                    // Validate and update if cumulative calculation is incorrect
                    soldeOpt.ifPresent(solde -> {
                        if (!estSoldeCumulatifCorrect(solde, employe)) {
                            System.out.println("Solde cumulatif incorrect détecté pour " + matriculeEmploye + ", recalcul en cours...");
                            recalculerCumulSolde(employe);
                        }
                    });

                    return soldeOpt;
                });
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
        // Calculate cumulative balance from ALL previous years if any
        float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, LocalDate.now().getYear());

        SoldeConge solde = new SoldeConge();
        solde.setEmploye(employe);
        solde.setSoldeAu2012(0f);
        solde.setCongesAcquisN(0f);
        solde.setRetardsN(0f);
        solde.setAutorisationsN(0f);
        solde.setSoldeActuel(cumulSoldePrecedent); // Set initial balance to cumulative from ALL years
        solde.setReport(cumulSoldePrecedent); // Set report to cumulative balance from ALL years
        solde.setAnnee(LocalDate.now().getYear());
        return solde;
    }

    /**
     * Calculate cumulative balance from ALL previous years up to (but not including) the specified year
     * This sums up the soldeActuel from ALL years before the current year
     */
    private float calculerCumulSoldeTotalPrecedent(Employe employe, int anneeCourante) {
        List<SoldeConge> soldes = soldeCongeRepository.findAllByEmployeOrderByAnneeDesc(employe);
        float cumul = 0f;

        for (SoldeConge solde : soldes) {
            if (solde.getAnnee() < anneeCourante) {
                cumul += solde.getSoldeActuel(); // Add the actual balance from ALL previous years
                System.out.println("Adding solde from year " + solde.getAnnee() + ": " + solde.getSoldeActuel() + " to cumulative. Total cumul: " + cumul);
            }
        }

        System.out.println("Final cumulative balance for employee " + employe.getMatricule() + " before year " + anneeCourante + ": " + cumul);
        return cumul;
    }

    /**
     * Calculate total cumulative balance up to and INCLUDING the specified year
     */
    private float calculerCumulSoldeTotal(Employe employe, int anneeLimite) {
        List<SoldeConge> soldes = soldeCongeRepository.findAllByEmployeOrderByAnneeDesc(employe);
        float cumul = 0f;

        for (SoldeConge solde : soldes) {
            if (solde.getAnnee() <= anneeLimite) {
                cumul += solde.getSoldeActuel(); // Add the actual balance from all years up to and including limite
                System.out.println("Adding solde from year " + solde.getAnnee() + ": " + solde.getSoldeActuel() + " to total cumulative. Total cumul: " + cumul);
            }
        }

        System.out.println("Final total cumulative balance for employee " + employe.getMatricule() + " up to year " + anneeLimite + ": " + cumul);
        return cumul;
    }

    public float calculerSoldeCumule(Employe employe, int annee) {
        List<SoldeConge> soldes = soldeCongeRepository.findAllByEmployeOrderByAnneeDesc(employe);
        float cumul = 0f;

        for (SoldeConge solde : soldes) {
            if (solde.getAnnee() <= annee) {
                cumul += solde.getSoldeActuel(); // Use actual balance instead of calculated fields
                System.out.println("Adding solde from year " + solde.getAnnee() + ": " + solde.getSoldeActuel() + " to calculated cumulative. Total cumul: " + cumul);
            }
        }

        System.out.println("Final calculated cumulative balance for employee " + employe.getMatricule() + " up to year " + annee + ": " + cumul);
        return cumul;
    }

    @Transactional
    @Override
    public SoldeConge initialiserSoldeAvecCumul(Employe employe) {
        int annee = LocalDate.now().getYear();
        // Calculate cumulative balance from ALL previous years
        float cumul = calculerCumulSoldeTotalPrecedent(employe, annee);

        SoldeConge nouveauSolde = SoldeConge.builder()
                .employe(employe)
                .annee(annee)
                .report(cumul) // Report the cumulative balance from ALL previous years
                .droitAnnuel(SoldeCongeCalculator.calculerDroitAnnuel(
                        employe.getGrade(),
                        employe.getTypeContrat().name(),
                        employe.getDateEmbauche()))
                .droitN(0f)
                .congesAcquisN(0f)
                .retardsN(0f)
                .autorisationsN(0f)
                .soldeActuel(cumul) // Initially = cumulative balance from ALL previous years
                .build();

        return soldeCongeRepository.save(nouveauSolde);
    }

    @Transactional
    @Override
    public SoldeConge recalculerCumulSolde(Employe employe) {
        int annee = LocalDate.now().getYear();
        Optional<SoldeConge> soldeOpt = soldeCongeRepository.findByEmployeAndAnnee(employe, annee);

        if (soldeOpt.isEmpty()) {
            throw new RuntimeException("Pas de solde trouvé pour l'année " + annee);
        }

        SoldeConge solde = soldeOpt.get();
        // Calculate cumulative balance from ALL previous years
        float cumul = calculerCumulSoldeTotalPrecedent(employe, annee);

        // Recalculate current balance: cumulative from all previous years + current year entitlements - current year deductions
        solde.setSoldeActuel(cumul + solde.getDroitAnnuel() - solde.getCongesAcquisN() - solde.getRetardsN() - solde.getAutorisationsN());
        solde.setReport(cumul);

        return soldeCongeRepository.save(solde);
    }



    ////////////////////////////////
    @Override
    @Transactional
    public SoldeConge forcerMiseAJourCumul(Employe employe) {
        Optional<SoldeConge> soldeOpt = getSoldeForEmployee(employe);
        if (soldeOpt.isPresent()) {
            return recalculerCumulSolde(employe);
        } else {
            return calculerEtMettreAJourSoldeActuel(employe);
        }
    }


    @Override
    @Transactional
    public SoldeConge getOrCreateSoldeActuel(String matriculeEmploye) {
        Employe employe = employeRepository.findByMatricule(matriculeEmploye)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé: " + matriculeEmploye));

        int currentYear = LocalDate.now().getYear();

        // Try to get current year balance
        Optional<SoldeConge> soldeOpt = soldeCongeRepository.findByEmployeAndAnnee(employe, currentYear);

        if (soldeOpt.isPresent()) {
            SoldeConge solde = soldeOpt.get();
            // Validate cumulative calculation
            if (!estSoldeCumulatifCorrect(solde, employe)) {
                System.out.println("Solde cumulatif incorrect détecté pour " + matriculeEmploye + ", recalcul en cours...");
                return recalculerCumulSolde(employe);
            }
            return solde;
        } else {
            // Create new balance for current year
            System.out.println("Création d'un nouveau solde pour l'année " + currentYear + " pour l'employé " + matriculeEmploye);
            return creerSoldePourAnneeCourante(employe, currentYear);
        }
    }


    @Transactional
    public SoldeConge creerSoldePourAnneeCourante(Employe employe, int annee) {
        // Calculate cumulative balance from ALL previous years
        float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, annee);

        float droitAnnuel = SoldeCongeCalculator.calculerDroitAnnuel(
                employe.getGrade(),
                employe.getTypeContrat().name(),
                employe.getDateEmbauche()
        );

        float droitN = (droitAnnuel / 12) * LocalDate.now().getMonthValue();

        SoldeConge nouveauSolde = SoldeConge.builder()
                .employe(employe)
                .annee(annee)
                .report(cumulSoldePrecedent)
                .droitAnnuel(droitAnnuel)
                .droitN(droitN)
                .congesAcquisN(0f)
                .retardsN(0f)
                .autorisationsN(0f)
                .soldeActuel(cumulSoldePrecedent + droitAnnuel) // Initial balance = cumulative + annual right
                .build();

        System.out.println("Nouveau solde créé pour " + employe.getMatricule() + " année " + annee);
        System.out.println("  - Cumul précédent: " + cumulSoldePrecedent);
        System.out.println("  - Droit annuel: " + droitAnnuel);
        System.out.println("  - Solde actuel: " + nouveauSolde.getSoldeActuel());

        return soldeCongeRepository.save(nouveauSolde);
    }

    /**
     * Check if the current balance is correctly calculated with cumulative logic
     */
    private boolean estSoldeCumulatifCorrect(SoldeConge soldeActuel, Employe employe) {
        try {
            // Calculate what the current balance SHOULD be based on cumulative logic
            float cumulSoldePrecedent = calculerCumulSoldeTotalPrecedent(employe, soldeActuel.getAnnee());
            float soldeCalcule = cumulSoldePrecedent
                    + soldeActuel.getDroitAnnuel()
                    - soldeActuel.getCongesAcquisN()
                    - soldeActuel.getRetardsN()
                    - soldeActuel.getAutorisationsN();

            // Compare with actual stored balance (allow small floating point differences)
            float difference = Math.abs(soldeCalcule - soldeActuel.getSoldeActuel());
            boolean isCorrect = difference < 0.01f; // Tolerance for floating point precision

            if (!isCorrect) {
                System.out.println("Incohérence détectée dans le solde cumulatif:");
                System.out.println("  - Solde stocké: " + soldeActuel.getSoldeActuel());
                System.out.println("  - Solde calculé: " + soldeCalcule);
                System.out.println("  - Cumul précédent: " + cumulSoldePrecedent);
                System.out.println("  - Différence: " + difference);
            }

            return isCorrect;

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du solde cumulatif: " + e.getMessage());
            return false; // If we can't verify, consider it incorrect
        }
    }
}