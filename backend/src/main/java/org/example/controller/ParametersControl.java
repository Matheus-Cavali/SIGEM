package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;
import org.example.dao.UsuarioDao;
import org.example.model.ParametrizacaoIgreja;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.sql.Connection;
import java.sql.SQLException;

public class ParametersControl {
    private static ParametersControl instancia;
    private final Gson gson = new Gson();

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
            try (Connection conn = Conexao.getConexao()) {
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1;
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    public Resposta buscar() {
        try (Connection conn = Conexao.getConexao()) {
            return new Resposta(200, gson.toJson(ParametrizacaoIgreja.buscar(conn)));
        } catch (Exception e) {
            return erro(500, "Erro ao buscar configuraÃ§Ãµes da igreja.");
        }
    }

    public Resposta salvar(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            ParametrizacaoIgreja parametros = gson.fromJson(json, ParametrizacaoIgreja.class);
            ParametrizacaoIgreja salvo = ParametrizacaoIgreja.salvar(conn, parametros);
            return new Resposta(200, gson.toJson(salvo));
        } catch (IllegalArgumentException e) {
            return erro(400, e.getMessage());
        } catch (Exception e) {
            return erro(500, "Erro ao salvar configuraÃ§Ãµes da igreja.");
        }
    }

    public Resposta salvarLogo(String auth, String caminhoLogo) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            ParametrizacaoIgreja salvo = ParametrizacaoIgreja.salvarLogo(conn, caminhoLogo);
            return new Resposta(200, gson.toJson(salvo));
        } catch (IllegalArgumentException e) {
            return erro(400, e.getMessage());
        } catch (Exception e) {
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
