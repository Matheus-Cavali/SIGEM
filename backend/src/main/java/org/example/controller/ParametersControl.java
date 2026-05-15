package org.example.controller;

import com.google.gson.Gson;
import org.example.exception.DatabaseException;
import org.example.facade.ParametersFacade;
import org.example.model.ParametrizacaoIgreja;
import org.example.model.Resposta;

public class ParametersControl {
    private static ParametersControl instancia;
    private final Gson gson = new Gson();
    private final ParametersFacade facade = new ParametersFacade();

    private ParametersControl() {}

    public static ParametersControl getInstancia() {
        if (instancia == null) instancia = new ParametersControl();
        return instancia;
    }

    public Resposta buscar() {
        try {
            return new Resposta(200, gson.toJson(facade.buscar()));
        } catch (DatabaseException e) {
            return erro(500, "Erro ao buscar configurações da igreja.");
        }
    }

    public Resposta salvar(String json) {
        try {
            ParametrizacaoIgreja parametros = gson.fromJson(json, ParametrizacaoIgreja.class);
            ParametrizacaoIgreja salvo = facade.salvar(parametros);
            return new Resposta(200, gson.toJson(salvo));
        } catch (IllegalArgumentException e) {
            return erro(400, e.getMessage());
        } catch (DatabaseException e) {
            return erro(500, "Erro ao salvar configurações da igreja.");
        }
    }

    public Resposta salvarLogo(String caminhoLogo) {
        try {
            ParametrizacaoIgreja salvo = facade.salvarLogo(caminhoLogo);
            return new Resposta(200, gson.toJson(salvo));
        } catch (IllegalArgumentException e) {
            return erro(400, e.getMessage());
        } catch (DatabaseException e) {
            return erro(500, "Erro ao salvar logo da igreja.");
        }
    }

    private Resposta erro(int status, String mensagem) {
        return new Resposta(status, "{\"erro\":\"" + escaparJson(mensagem) + "\"}");
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
