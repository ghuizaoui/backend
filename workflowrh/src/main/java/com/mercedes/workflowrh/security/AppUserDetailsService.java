package com.mercedes.workflowrh.security;

import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@Service
public class AppUserDetailsService implements UserDetailsService {
    @Autowired
    private EmployeRepository employeRepository;

    @Override
    public UserDetails loadUserByUsername(String matricule) throws UsernameNotFoundException {
        Employe emp = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UsernameNotFoundException("Matricule non trouvé : " + matricule));

        // CORRECTION ICI
        return User.builder()
                .username(emp.getMatricule())
                .password(emp.getMotDePasse())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + emp.getRole().name())))
                .build();
    }
}