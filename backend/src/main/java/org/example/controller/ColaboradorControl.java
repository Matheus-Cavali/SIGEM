package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;

import org.example.model.Colaborador;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ColaboradorControl {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

    private static ColaboradorControl instancia;

    private ColaboradorControl() {}

    public static synchronized ColaboradorControl getInstancia() {
        if (instancia == null) instancia = new ColaboradorControl();
        return instancia;
    }

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
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        if ("nome".equalsIgnoreCase(kv[0])) nome = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        if ("email".equalsIgnoreCase(kv[0])) email = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    }
                }
            }
            List<Colaborador> lista = Colaborador.listar(conn, nome, email);
            lista.forEach(c -> c.setSenha(null));
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar colaboradores.\"}");
        }
    }

    public Resposta buscarPorId(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_USUARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            Colaborador c = Colaborador.buscarPorId(conn, id);
            if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");

            c.setSenha(null);
            return new Resposta(200, gson.toJson(c));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar colaborador.\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        if (!usuarioTemPermissao(auth, "GESTAO_USUARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
            Colaborador c = Colaborador.buscarPorId(conn, id);

            if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");

            if (body.has("nome") && !body.get("nome").isJsonNull()) c.setNome(body.get("nome").getAsString());
            if (body.has("email") && !body.get("email").isJsonNull()) c.setEmail(body.get("email").getAsString());
            if (body.has("celular") && !body.get("celular").isJsonNull()) c.setCelular(body.get("celular").getAsString());

            Colaborador.atualizar(conn, c);
            return new Resposta(200, "{\"mensagem\":\"Colaborador atualizado\"}");
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar colaborador.\"}");
        }
    }
}
