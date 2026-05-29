package org.example.util;

import org.mindrot.jbcrypt.BCrypt;

public class Criptografia {

    public static String hashSenha(String senhaAberta) {

        return BCrypt.hashpw(senhaAberta, BCrypt.gensalt());
    }


    public static boolean verificarSenha(String senhaAberta, String senhaDoBanco) {
        return BCrypt.checkpw(senhaAberta, senhaDoBanco);
    }
}