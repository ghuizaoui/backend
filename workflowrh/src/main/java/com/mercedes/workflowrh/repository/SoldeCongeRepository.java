package com.mercedes.workflowrh.repository;

import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.entity.Employe;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {



    // Trouver le solde d’un employé (dernier solde)


    // Trouver le solde d’un employé pour une année donnée
    Optional<SoldeConge> findByEmployeAndAnnee(Employe employe, Integer annee);

    // Récupérer l’historique des soldes d’un employé (ordonné par année décroissante)
    List<SoldeConge> findAllByEmployeOrderByAnneeDesc(Employe employe);

    // Vérifier si un employé a un solde négatif (bloquer nouvelles demandes)
    List<SoldeConge> findByEmployeAndSoldeActuelLessThan(Employe employe, Float seuil);

    // Récupérer tous les soldes d’une année donnée
    List<SoldeConge> findAllByAnnee(Integer annee);



    // Dans SoldeCongeRepository.java
    Optional<SoldeConge> findTopByEmployeMatriculeOrderByAnneeDesc(String matricule);









    @Query("SELECT sc FROM SoldeConge sc WHERE sc.employe = :employe")
    Optional<SoldeConge> findByEmploye(@Param("employe") Employe employe);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sc FROM SoldeConge sc WHERE sc.employe = :employe")
    Optional<SoldeConge> findByEmployeWithLock(@Param("employe") Employe employe);

    @Query("SELECT sc FROM SoldeConge sc WHERE sc.employe = :employe")
    List<SoldeConge> findAllByEmploye(@Param("employe") Employe employe);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sc FROM SoldeConge sc WHERE sc.employe = :employe")
    List<SoldeConge> findAllByEmployeWithLock(@Param("employe") Employe employe);



}
