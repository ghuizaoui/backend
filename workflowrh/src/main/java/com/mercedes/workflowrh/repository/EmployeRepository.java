package com.mercedes.workflowrh.repository;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, String> {
    Optional<Employe> findByMatricule(String matricule);
    Optional<Employe> findByEmail(String email);

    @Query(value = "SELECT matricule FROM employes WHERE matricule LIKE CONCAT(:prefix, '%') ORDER BY matricule DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLastMatriculeWithPrefix(@Param("prefix") String prefix);
    List<Employe> findByRole(Role role);
    List<Employe> findAllByRole(Role role);
    // Add these methods to your EmployeRepository

    List<Employe> findByService(String service);
    long count();
    Optional<Employe> findByServiceAndChefLevel(String m , long chefLevel);


    Employe getEmployeByMatricule(String matricule);


    List<Employe> findByServiceAndRole(String service, Role role);
    boolean existsByMatricule(String matricule);




    // Add this method to find super DRH
    @Query("SELECT e FROM Employe e WHERE e.drhSuper = true")
    List<Employe> findByDrhSuperTrue();

    // Add this method to find DRH by service
    @Query("SELECT e FROM Employe e WHERE e.role = 'DRH' AND e.service = :service")
    List<Employe> findDrhByService(@Param("service") String service);
}
