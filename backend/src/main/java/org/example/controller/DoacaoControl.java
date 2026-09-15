package org.example.controller;

public class DoacaoControl {

    private static DoacaoControl instancia;

    private DoacaoControl() {}

    public static synchronized DoacaoControl getInstancia() {
        if (instancia == null) instancia = new DoacaoControl();
        return instancia;
    }

    // GERENCIA ROTAS
}
