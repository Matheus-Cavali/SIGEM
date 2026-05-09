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
}