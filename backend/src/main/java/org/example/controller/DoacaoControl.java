package org.example.controller;

import org.example.model.Resposta;
import org.example.strategy.DoacaoStrategy;

public class DoacaoControl {

    private static DoacaoControl instancia;

    private DoacaoStrategy inicioDaCadeia;

    private DoacaoControl() {
        DoacaoStrategy material = DoacaoMaterialControl.getInstancia();
        DoacaoStrategy financeira = DoacaoFinanceiraControl.getInstancia();

        material.setProxima(financeira);

        this.inicioDaCadeia = material;
    }

    public static synchronized DoacaoControl getInstancia() {
        if (instancia == null) instancia = new DoacaoControl();
        return instancia;
    }

    public Resposta lancar(String auth, String json) {
        Resposta resposta = inicioDaCadeia.cadastrar(auth, json);
        return resposta;
    }
}