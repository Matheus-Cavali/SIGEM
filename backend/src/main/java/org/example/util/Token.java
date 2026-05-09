package org.example.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;

public class Token{
    private static final String SECRET_KEY = System.getenv("JWT_SECRET");

    static {
        if (SECRET_KEY == null) {
            System.err.println("ERRO CRÍTICO: A variável JWT_SECRET não foi configurada!");
        }
    }

    public static String gerarToken(String email) {
        return JWT.create()
                .withSubject(email)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // Expira em 1 hora
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public static String validarToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}