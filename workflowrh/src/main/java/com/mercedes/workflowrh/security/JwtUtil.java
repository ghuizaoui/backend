package com.mercedes.workflowrh.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.secret}")
    private String jwtRefreshSecret;

    @Value("${jwt.refresh.expiration}")
    private long jwtRefreshExpirationMs;

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtRefreshSecret)
                .compact();
    }

    public String getUsernameFromToken(String token, boolean refresh) {
        String secret = refresh ? jwtRefreshSecret : jwtSecret;
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, boolean refresh) {
        try {
            String secret = refresh ? jwtRefreshSecret : jwtSecret;
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return !isTokenExpired(token, refresh);
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Token validation error: " + e.getMessage());
            return false;
        }
    }

    // Méthode helper pour vérifier l'expiration
    private boolean isTokenExpired(String token, boolean refresh) {
        Date expiration = getExpirationDateFromToken(token, refresh);
        return expiration.before(new Date());
    }

    // Méthode pour obtenir la date d'expiration
    public Date getExpirationDateFromToken(String token, boolean refresh) {
        String secret = refresh ? jwtRefreshSecret : jwtSecret;
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    // Méthode pour vérifier si le token expire bientôt (dans les 5 prochaines minutes)
    public boolean isTokenExpiringSoon(String token, boolean refresh) {
        try {
            Date expiration = getExpirationDateFromToken(token, refresh);
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            return timeUntilExpiration < (5 * 60 * 1000); // 5 minutes
        } catch (Exception e) {
            return true;
        }
    }

    // Méthode pour debug - obtenir les infos du token
    public void debugToken(String token, boolean refresh) {
        try {
            String secret = refresh ? jwtRefreshSecret : jwtSecret;
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            System.out.println("=== TOKEN DEBUG ===");
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expires At: " + claims.getExpiration());
            System.out.println("Current Time: " + new Date());
            System.out.println("Is Expired: " + claims.getExpiration().before(new Date()));
            System.out.println("Time until expiry: " + (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000 / 60 + " minutes");
            System.out.println("==================");
        } catch (Exception e) {
            System.err.println("Token debug error: " + e.getMessage());
        }
    }
}