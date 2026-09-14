package com.f1rsters.tech_challenge_mecanica.lambda;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtService {

    private static final long EXPIRATION_TIME = 15 * 60 * 1000;

    private final SecretKey secretKey;

    private JwtService(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public static JwtService create() {
        String jwtSecretBase64 = System.getenv("JWT_SECRET");

        if (jwtSecretBase64 == null || jwtSecretBase64.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable not set"
            );
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(jwtSecretBase64);
            SecretKey secretKey = Keys.hmacShaKeyFor(decodedKey);

            return new JwtService(secretKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT_SECRET contains an invalid Base64 or JWT key",
                    e
            );
        }
    }
    
    public String generateToken(ClientInfo client) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", client.getId());
        claims.put("cpf", client.getCpf());
        claims.put("nome", client.getNome());
        claims.put("status", client.getStatus());
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .claims(claims)
                .subject(client.getCpf())
                .issuedAt(now)
                .expiration(expiration)
                .issuer("tech-challenge-auth-lambda")
                .audience().add("tech-challenge-api").and()
                .signWith(secretKey)
                .compact();
    }
    
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid token: " + e.getMessage(),
                    e
            );
        }
    }
}