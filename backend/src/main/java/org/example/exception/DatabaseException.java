package org.example.exception;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String mensagem) {
        super(mensagem);
    }

    public DatabaseException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}