package org.example.util;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Data {

    public static LocalDate converterPtBrParaLocalDate(String dataTexto) {
        try {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("ddMMyyyy");
            return LocalDate.parse(dataTexto, formato);
        } catch (DateTimeParseException e) {
            System.err.println("Formato de data inválido! Digite 8 números (DDMMYYYY).");
            return null;
        }
    }

    public static LocalDate parseFlexivel(String dataTexto) {
        if (dataTexto == null || dataTexto.trim().isEmpty()) return null;

        String limpa = dataTexto.replaceAll("[^0-9]", "");
        if (limpa.length() != 8) return null;

        String formatada = limpa.substring(0, 2) + "/" + limpa.substring(2, 4) + "/" + limpa.substring(4, 8);

        try {
            return LocalDate.parse(formatada, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dataTexto, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}