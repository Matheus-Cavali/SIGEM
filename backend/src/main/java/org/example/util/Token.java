package org.example.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class Token {
    private static String SECRET_KEY;

    static {
        try {
            Properties props = new Properties();

            InputStream input = Token.class.getResourceAsStream("/properties/config.properties");

            if (input == null) {
                throw new RuntimeException("Arquivo config.properties não encontrado em resources/properties/");
            }

            props.load(input);
            input.close();

            SECRET_KEY = props.getProperty("jwt.secret");

            if (SECRET_KEY == null || SECRET_KEY.trim().isEmpty()) {
                System.err.println("ERRO CRÍTICO: A variável jwt.secret não foi configurada no config.properties!");
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar a chave do Token: " + e.getMessage());
        }
    }

    public static String gerarToken(String email, int nivelAcesso, String tipoUsuario) {
        return JWT.create()
                .withSubject(email)
                .withClaim("nivelAcesso", nivelAcesso)
                .withClaim("tipoUsuario", tipoUsuario)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
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

    public static int extrairNivelAcesso(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET_KEY)).build().verify(token);
            return jwt.getClaim("nivelAcesso").asInt();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String extrairTipoUsuario(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET_KEY)).build().verify(token);
            return jwt.getClaim("tipoUsuario").asString();
        } catch (Exception e) {
            return null;
        }
    }
}