package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;

import java.util.List;
import java.util.Optional;

public interface EmployeService {
    Employe ajouterEmploye(EmployeDTO dto);
    void changerMotDePassePremiereConnexion(String matricule, String nouveauMotDePasse);
    List<Employe> getAllEmployes();
    Optional<Employe> getEmployeByMatricule(String matricule);
    Employe updateEmploye(String matricule, EmployeDTO dto);
    Employe updateEmploye(Employe employe);
    Optional<Employe> getEmployeProfile(String matricule);

}


