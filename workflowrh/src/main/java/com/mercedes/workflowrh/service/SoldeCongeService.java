package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.SoldeConge;

import java.util.Optional;

public interface SoldeCongeService {
    SoldeConge calculerEtMettreAJourSoldeActuel(Employe employe);
    Optional<SoldeConge> getSoldeActuel(String matriculeEmploye);
    void debiterSoldeConge(Employe employe, double jours);
    void crediterSoldeConge(Employe employe, double jours);
}
