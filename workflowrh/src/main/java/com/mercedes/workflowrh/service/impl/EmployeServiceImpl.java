package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import jakarta.mail.MessagingException;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Data


@Service
@RequiredArgsConstructor
public class EmployeServiceImpl implements EmployeService {
    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(\\d+)$");

    private final SoldeCongeService soldeCongeService;

    @Override
    @Transactional
    public Employe ajouterEmploye(EmployeDTO dto) {
        String matricule = genererMatricule(dto.getRole(), dto.getPrenom(), dto.getNom());
        String rawPassword = genererMotDePasse();

        Employe emp = Employe.builder()
                .matricule(matricule)
                .motDePasse(passwordEncoder.encode(rawPassword))
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .direction(dto.getDirection())
                .service(dto.getService())
                .grade(dto.getGrade())
                .dateEmbauche(dto.getDateEmbauche() != null ? dto.getDateEmbauche() : LocalDate.now())
                .typeContrat(dto.getTypeContrat())
                .role(dto.getRole())
                .premiereConnexion(true)
                .build();

        employeRepository.save(emp);

        // Initialiser son solde de congé dès la création
        soldeCongeService.calculerEtMettreAJourSoldeActuel(emp);

        try {
            String html = mailService.buildBienvenueMail(emp.getPrenom(), emp.getNom(), matricule, rawPassword);
            mailService.sendHtmlMail(emp.getEmail(), "Bienvenue sur Workflow RH", html);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return emp;
    }
    @Override
    @Transactional
    public void changerMotDePassePremiereConnexion(String matricule, String nouveauMotDePasse) {
        Employe emp = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        emp.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        emp.setPremiereConnexion(false);
        employeRepository.save(emp);
    }

    private String genererMatricule(Role role, String prenom, String nom) {
        String prefixRole = role.name();
        String first3Pre = nettoyer(prenom).substring(0, 3);
        String first3Nom = nettoyer(nom).substring(0, 3);
        String prefix = prefixRole + first3Pre + first3Nom;

        Optional<String> last = employeRepository.findLastMatriculeWithPrefix(prefix);
        long next = 1;
        if (last.isPresent()) {
            Matcher m = SUFFIX_PATTERN.matcher(last.get());
            if (m.find()) next = Long.parseLong(m.group(1)) + 1;
        }
        return prefix + String.format("%03d", next);
    }

    private String nettoyer(String s) {
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^A-Za-z]", "")
                .toLowerCase();
        return (n.length() >= 3) ? n : String.format("%-3s", n).replace(' ', 'x');
    }

    private String genererMotDePasse() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public List<Employe> getAllEmployes() {
        return employeRepository.findAll();
    }

    @Override
    public Optional<Employe> getEmployeByMatricule(String matricule) {
        return employeRepository.findByMatricule(matricule);
    }


    @Override
    @Transactional
    public Employe updateEmploye(String matricule, EmployeDTO dto) {
        Employe employe = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        employe.setNom(dto.getNom());
        employe.setPrenom(dto.getPrenom());
        employe.setEmail(dto.getEmail());
        employe.setDirection(dto.getDirection());
        employe.setService(dto.getService());
        employe.setGrade(dto.getGrade());
        employe.setDateEmbauche(dto.getDateEmbauche());
        employe.setTypeContrat(dto.getTypeContrat());
        employe.setRole(dto.getRole());
        return employeRepository.save(employe);
    }

    @Override
    public Employe updateEmploye(Employe employe) {
        return  employeRepository.save(employe);
    }

    public Optional<Employe> getEmployeProfile(String matricule) {
        return employeRepository.findByMatricule(matricule);
    }


}
