package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;

import java.util.Optional;

public interface SoldeCongeService {
    SoldeConge calculerEtMettreAJourSoldeActuel(Employe employe);
    Optional<SoldeConge> getSoldeActuel(String matriculeEmploye);
    void debiterSoldeConge(Employe employe, double jours);
    void crediterSoldeConge(Employe employe, double jours);




    boolean estSoldeNegatif(Employe employe); // vérifier si solde < 0
    void verrouillerSiSoldeNegatif(Employe employe); // empêche nouvelles demandes
    float calculerDroitMensuel(Employe employe); // (droit annuel / 12)
    void reinitialiserSoldeAnnuel(Employe employe, int nouvelleAnnee); // passage N -> N+1


    SoldeConge creerSoldeConge(Employe employe);
}
