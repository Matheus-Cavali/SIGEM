package org.example.strategy;

import org.example.model.Resposta;

public interface DoacaoStrategy {

    void setProxima(DoacaoStrategy proxima);

    Resposta cadastrar(String auth, String json);
}