package org.example.sigem.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JWTTokenProvider {
    private static final String SECRET = "SigemIgrejaChaveMestraSecret2026";
    private static final long EXPIRATION_TIME = 28800000; // 8 horas

    public static String generateToken(String cpf, String tipoUsuario) {
        return Jwts.builder()
                .setSubject(cpf)
                .claim("role", tipoUsuario)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    public static boolean verifyToken(String token) {
        try {
            // Remove a palavra "Bearer " se ela vier no header
            String cleanToken = token.replace("Bearer ", "");
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(cleanToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}