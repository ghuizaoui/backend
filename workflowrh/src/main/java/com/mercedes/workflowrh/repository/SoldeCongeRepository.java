package com.mercedes.workflowrh.repository;

import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {



    // Trouver le solde d’un employé (dernier solde)
    Optional<SoldeConge> findByEmploye(Employe employe);

    // Trouver le solde d’un employé pour une année donnée
    Optional<SoldeConge> findByEmployeAndAnnee(Employe employe, Integer annee);

    // Récupérer l’historique des soldes d’un employé (ordonné par année décroissante)
    List<SoldeConge> findAllByEmployeOrderByAnneeDesc(Employe employe);

    // Vérifier si un employé a un solde négatif (bloquer nouvelles demandes)
    List<SoldeConge> findByEmployeAndSoldeActuelLessThan(Employe employe, Float seuil);

    // Récupérer tous les soldes d’une année donnée
    List<SoldeConge> findAllByAnnee(Integer annee);
}
