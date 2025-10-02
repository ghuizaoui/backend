package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.entity.CategorieDemande;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;

import java.util.Optional;

public interface SoldeCongeService {
    SoldeConge calculerEtMettreAJourSoldeActuel(Employe employe);
    void debiterSoldeConge(Employe employe, double jours);
    void debiterSoldeConge(Employe employe, double jours, CategorieDemande categorie); // New overload
    boolean estSoldeNegatif(Employe employe);
    void verrouillerSiSoldeNegatif(Employe employe);
    float calculerDroitMensuel(Employe employe);
    void reinitialiserSoldeAnnuel(Employe employe, int nouvelleAnnee);
    SoldeConge creerSoldeConge(Employe employe);
    Optional<SoldeConge> getSoldeActuel(String matriculeEmploye);
    void crediterSoldeConge(Employe employe, double jours);


    public SoldeConge recalculerCumulSolde(Employe employe);
    public SoldeConge initialiserSoldeAvecCumul(Employe employe);
    public SoldeConge forcerMiseAJourCumul(Employe employe);
    public SoldeConge getOrCreateSoldeActuel(String matriculeEmploye);
}