package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.*;
import com.mercedes.workflowrh.entity.Demande;
import com.mercedes.workflowrh.entity.Employe;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.security.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private AppUserDetailsService userDetailsService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmployeRepository employeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest request) {

        /* 1. on vérifie que le matricule existe */
        Employe emp = employeRepository.findByMatricule(request.getMatricule())
                .orElseThrow(() -> new UsernameNotFoundException("Matricule inconnu"));

        /* 2. Première connexion ? → 423 Locked + corps JSON */
        if (Boolean.TRUE.equals(emp.getPremiereConnexion())) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "firstLogin", true,
                            "matricule",   emp.getMatricule()
                    ));
        }

        /* 3. Authentification Spring Security */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getMatricule(), request.getMotDePasse()));

        /* 4. Génération des tokens */
        UserDetails ud   = userDetailsService.loadUserByUsername(request.getMatricule());
        String access    = jwtUtil.generateAccessToken(ud);
        String refresh   = jwtUtil.generateRefreshToken(ud);

        return ResponseEntity.ok(
                new LoginResponse(access, refresh, emp.getRole().name()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken, true))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token invalide ou expiré");
        String username = jwtUtil.getUsernameFromToken(refreshToken, true);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        return ResponseEntity.ok(new RefreshTokenResponse(accessToken));
    }

    /* … imports … */
    @PostMapping("/change-password")
    public ResponseEntity<LoginResponse> changePassword(
            @RequestBody ChangePasswordRequest dto) {

        Employe emp = employeRepository.findByMatricule(dto.getMatricule())
                .orElseThrow(() -> new UsernameNotFoundException("Matricule inconnu"));

        if (Boolean.FALSE.equals(emp.getPremiereConnexion()))
            return ResponseEntity.badRequest().build();

        /* on met à jour le mot de passe */
        emp.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        emp.setPremiereConnexion(false);
        employeRepository.save(emp);

        /* on génère directement les tokens pour éviter un 2ᵉ appel /login */
        UserDetails ud = userDetailsService.loadUserByUsername(emp.getMatricule());
        String access  = jwtUtil.generateAccessToken(ud);
        String refresh = jwtUtil.generateRefreshToken(ud);

        return ResponseEntity.ok(
                new LoginResponse(access, refresh, emp.getRole().name()));
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token, false)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou expiré");
        }
        String username = jwtUtil.getUsernameFromToken(token, false);
        Employe emp = employeRepository.findByMatricule(username)
                .orElseThrow(() -> new UsernameNotFoundException("Employé non trouvé"));
        return ResponseEntity.ok(emp);
    }


    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Demande demande;

    @ManyToOne
    @JoinColumn(name = "destinataire_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Employe destinataire;

}