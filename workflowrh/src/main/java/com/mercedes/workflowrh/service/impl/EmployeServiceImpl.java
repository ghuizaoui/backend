package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.EmployeDTO;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.service.EmployeService;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmployeServiceImpl implements EmployeService {

    private final EmployeRepository employeRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SoldeCongeService soldeCongeService;

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(\\d+)$");

    @Override
    @Transactional
    public Employe ajouterEmploye(EmployeDTO dto) {
        // Validate input
        validateEmployeDTO(dto);

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
                .chefLevel(dto.getChefLevel())
                .estBanni(false)
                .drhSuper(false)
                .build();

        // Assign existing chefs if available and employee is not a chef
        if (dto.getService() != null && !dto.getService().isEmpty() && (dto.getChefLevel() == null || dto.getRole() != Role.CHEF)) {
            assignExistingChefs(emp, dto.getService());
        }

        emp = employeRepository.save(emp);

        // Handle chef level logic (if emp is a chef)
        handleChefLevel(emp, null);

        // Initialize leave balance
        soldeCongeService.calculerEtMettreAJourSoldeActuel(emp);

        // Send welcome email
        try {
            String html = mailService.buildBienvenueMail(emp.getPrenom(), emp.getNom(), matricule, rawPassword);
            mailService.sendHtmlMail(emp.getEmail(), "Bienvenue sur Workflow RH", html);
        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email for " + matricule + ": " + e.getMessage());
        }

        return emp;
    }

    @Override
    @Transactional
    public void changerMotDePassePremiereConnexion(String matricule, String nouveauMotDePasse) {
        Employe emp = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));
        emp.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        emp.setPremiereConnexion(false);
        employeRepository.save(emp);
    }

    private String genererMatricule(Role role, String prenom, String nom) {
        String prefixRole = role.name();
        String first3Pre = nettoyer(prenom).substring(0, Math.min(3, prenom.length()));
        String first3Nom = nettoyer(nom).substring(0, Math.min(3, nom.length()));
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
        validateEmployeDTO(dto);

        Employe emp = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));

        String oldService = emp.getService();
        Integer oldChefLevel = emp.getChefLevel();

        emp.setNom(dto.getNom());
        emp.setPrenom(dto.getPrenom());
        emp.setEmail(dto.getEmail());
        emp.setDirection(dto.getDirection());
        emp.setService(dto.getService());
        emp.setGrade(dto.getGrade());
        emp.setDateEmbauche(dto.getDateEmbauche());
        emp.setTypeContrat(dto.getTypeContrat());
        emp.setRole(dto.getRole());
        emp.setChefLevel(dto.getChefLevel());

        emp = employeRepository.save(emp);

        // Handle chef level logic and service changes
        handleChefLevel(emp, oldService);

        // Assign existing chefs if service changed and employee is not a chef
        if (!oldService.equals(dto.getService()) && (dto.getChefLevel() == null || dto.getRole() != Role.CHEF)) {
            assignExistingChefs(emp, dto.getService());
        }

        return emp;
    }

    @Override
    @Transactional
    public Employe updateEmploye(Employe employe) {
        String oldService = employeRepository.findByMatricule(employe.getMatricule())
                .map(Employe::getService)
                .orElse(null);
        employe = employeRepository.save(employe);
        handleChefLevel(employe, oldService);
        return employe;
    }

    @Override
    public Optional<Employe> getEmployeProfile(String matricule) {
        return employeRepository.findByMatricule(matricule);
    }

    private void validateEmployeDTO(EmployeDTO dto) {
        if (dto.getNom() == null || dto.getNom().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom est requis");
        }
        if (dto.getPrenom() == null || dto.getPrenom().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prénom est requis");
        }
        if (dto.getEmail() == null || !dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email invalide");
        }
        if (dto.getService() == null || dto.getService().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service est requis");
        }
        if (dto.getChefLevel() != null && dto.getRole() != Role.CHEF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChefLevel ne peut être défini que pour un rôle CHEF");
        }
        if (dto.getChefLevel() != null && (dto.getChefLevel() != 1 && dto.getChefLevel() != 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChefLevel doit être 1 ou 2");
        }
    }

    private void assignExistingChefs(Employe emp, String service) {
        if (emp.getChefLevel() != null) return; // Skip if employee is a chef

        // Assign chef 1 if exists and not already assigned
        employeRepository.findByServiceAndChefLevel(service, 1)
                .filter(chef -> emp.getChefHierarchique1Matricule() == null || !emp.getChefHierarchique1Matricule().equals(chef.getMatricule()))
                .ifPresent(chef -> emp.setChefHierarchique1Matricule(chef.getMatricule()));

        // Assign chef 2 if exists and not already assigned
        employeRepository.findByServiceAndChefLevel(service, 2)
                .filter(chef -> emp.getChefHierarchique2Matricule() == null || !emp.getChefHierarchique2Matricule().equals(chef.getMatricule()))
                .ifPresent(chef -> emp.setChefHierarchique2Matricule(chef.getMatricule()));

        if (emp.getChefHierarchique1Matricule() != null || emp.getChefHierarchique2Matricule() != null) {
            employeRepository.save(emp);
        }
    }

    private void handleChefLevel(Employe emp, String oldService) {
        Integer level = emp.getChefLevel();
        String service = emp.getService();

        // If not a chef, clear their chef assignment from other employees if they were previously a chef
        if (level == null || emp.getRole() != Role.CHEF) {
            if (oldService != null && !oldService.isEmpty()) {
                clearChefAssignments(emp.getMatricule(), oldService);
            }
            return;
        }

        // Validate service
        if (service == null || service.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service doit être spécifié pour les chefs");
        }

        // Check for existing chef at the same level
        Optional<Employe> existingChef = employeRepository.findByServiceAndChefLevel(service, level);
        if (existingChef.isPresent() && !existingChef.get().getMatricule().equals(emp.getMatricule())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un chef de niveau " + level + " existe déjà pour le service " + service + ": " + existingChef.get().getMatricule());
        }

        // Clear previous chef assignments if service changed
        if (oldService != null && !oldService.equals(service)) {
            clearChefAssignments(emp.getMatricule(), oldService);
        }

        // Update employees in the service to assign the new chef
        List<Employe> serviceEmployees = employeRepository.findByService(service);
        for (Employe e : serviceEmployees) {
            if (!e.getMatricule().equals(emp.getMatricule())) { // Exclude self
                boolean updated = false;
                if (level == 1 && (e.getChefHierarchique1Matricule() == null || e.getChefHierarchique1Matricule().equals(emp.getMatricule()))) {
                    e.setChefHierarchique1Matricule(emp.getMatricule());
                    updated = true;
                } else if (level == 2 && (e.getChefHierarchique2Matricule() == null || e.getChefHierarchique2Matricule().equals(emp.getMatricule()))) {
                    e.setChefHierarchique2Matricule(emp.getMatricule());
                    updated = true;
                }
                if (updated) {
                    employeRepository.save(e);
                }
            }
        }
    }

    private void clearChefAssignments(String matricule, String service) {
        List<Employe> serviceEmployees = employeRepository.findByService(service);
        for (Employe e : serviceEmployees) {
            boolean updated = false;
            if (matricule.equals(e.getChefHierarchique1Matricule())) {
                e.setChefHierarchique1Matricule(null);
                updated = true;
            }
            if (matricule.equals(e.getChefHierarchique2Matricule())) {
                e.setChefHierarchique2Matricule(null);
                updated = true;
            }
            if (updated) {
                employeRepository.save(e);
            }
        }
    }
}
