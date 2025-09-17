package com.mercedes.workflowrh.repository;

import com.mercedes.workflowrh.entity.SoldeConge;
import com.mercedes.workflowrh.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SoldeCongeRepository extends JpaRepository<SoldeConge, Long> {
    Optional<SoldeConge> findByEmploye(Employe employe);
}
