package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;

import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.model.Voluntario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

public class VoluntarioControl {

    private final Gson gson = new Gson();

    public VoluntarioControl() {}

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        String email = emailDoToken(auth);
        if (email != null) {
            try {
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public Resposta listar(String auth, String query) {
        if (!usuarioTemPermissao(auth, "GESTAO_USUARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            String nome = null, email = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "nome".equalsIgnoreCase(pair[0]))
                        nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    if (pair.length == 2 && "email".equalsIgnoreCase(pair[0]))
                        email = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
            List<Voluntario> lista = Voluntario.listar(conn, nome, email);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar voluntários.\"}");
        }
    }

    public Resposta buscarPorId(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_USUARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            Voluntario v = Voluntario.buscarPorId(conn, id);
            if (v == null) return new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");
            return new Resposta(200, gson.toJson(v));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar voluntário.\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        if (!usuarioTemPermissao(auth, "GESTAO_USUARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
            Voluntario v = Voluntario.buscarPorId(conn, id);

            if (v == null) return new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");

            if (body.has("nome") && !body.get("nome").isJsonNull()) v.setNome(body.get("nome").getAsString());
            if (body.has("email") && !body.get("email").isJsonNull()) v.setEmail(body.get("email").getAsString());
            if (body.has("celular") && !body.get("celular").isJsonNull()) v.setCelular(body.get("celular").getAsString());

            Voluntario.atualizar(conn, v);
            return new Resposta(200, "{\"mensagem\":\"Voluntário atualizado\"}");
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar voluntário.\"}");
        }
    }
}
