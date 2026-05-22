package org.example.util;

public class CpfUtil {

    public static boolean validar(String cpf) {
        boolean valido = false;
        if (cpf != null) {
            String digits = cpf.replaceAll("[^0-9]", "");
            if (digits.length() == 11 && !digits.chars().allMatch(c -> c == digits.charAt(0))) {
                try {
                    int sum = 0;
                    for (int i = 0; i < 9; i++) {
                        sum += (digits.charAt(i) - '0') * (10 - i);
                    }
                    int dig1 = 11 - (sum % 11);
                    if (dig1 > 9) dig1 = 0;

                    sum = 0;
                    for (int i = 0; i < 10; i++) {
                        sum += (digits.charAt(i) - '0') * (11 - i);
                    }
                    int dig2 = 11 - (sum % 11);
                    if (dig2 > 9) dig2 = 0;

                    valido = (digits.charAt(9) - '0' == dig1) && (digits.charAt(10) - '0' == dig2);
                } catch (Exception e) {
                    valido = false;
                }
            }
        }
        return valido;
    }

    public static String formatar(String cpf) {
        String digits = cpf.replaceAll("[^0-9]", "");
        String formatado;
        if (digits.length() == 11) {
            formatado = digits.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else {
            formatado = cpf;
        }
        return formatado;
    }
}
