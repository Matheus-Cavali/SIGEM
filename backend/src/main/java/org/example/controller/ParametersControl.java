package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.ConexaoSingleton;
import org.example.exception.DatabaseException;
import org.example.facade.ParametersFacade;
import org.example.model.ParametrizacaoIgreja;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.dao.UsuarioDao;

import java.sql.Connection;
import java.sql.SQLException;

public class ParametersControl {
    private static ParametersControl instancia;
    private final Gson gson = new Gson();
    private final ParametersFacade facade = new ParametersFacade();

    private ParametersControl() {}

    public static ParametersControl getInstancia() {
        if (instancia == null) instancia = new ParametersControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1;
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    public Resposta buscar() {
        try {
            return new Resposta(200, gson.toJson(facade.buscar()));
        } catch (DatabaseException e) {
            return erro(500, "Erro ao buscar configurações da igreja.");
        }
    }

    public Resposta salvar(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
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

    public Resposta salvarLogo(String auth, String caminhoLogo) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
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
