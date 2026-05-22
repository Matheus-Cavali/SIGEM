package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.Colaborador;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ColaboradorControl {

    private static ColaboradorControl instancia;

    private ColaboradorControl() {}

    public static ColaboradorControl getInstancia() {
        if (instancia == null) instancia = new ColaboradorControl();
        return instancia;
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        boolean permitido = false;
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = Conexao.getConexao()) {
                UsuarioDao uDao = new UsuarioDao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return permitido;
    }

    public Resposta listar(String auth, String query) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
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
            List<Colaborador> lista = new ColaboradorDao().listar(conn, nome, email);
            lista.forEach(c -> c.setSenha(null)); // Protegendo a senha
            return new Resposta(200, gson.toJson(lista));
        } catch (SQLException e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar colaboradores.\"}");
        }
    }

    public Resposta buscarPorId(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            Colaborador c = new ColaboradorDao().buscarPorId(conn, id);
            if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");

            c.setSenha(null); // Protegendo a senha
            return new Resposta(200, gson.toJson(c));
        } catch (SQLException e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar colaborador.\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            Colaborador body = gson.fromJson(jsonBody, Colaborador.class);
            ColaboradorDao dao = new ColaboradorDao();
            Colaborador c = dao.buscarPorId(conn, id);

            if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");

            if (body.getNome() != null) c.setNome(body.getNome());
            if (body.getEmail() != null) c.setEmail(body.getEmail());
            if (body.getCelular() != null) c.setCelular(body.getCelular());

            if (dao.atualizar(conn, c)) {
                return new Resposta(200, "{\"mensagem\":\"Colaborador atualizado\"}");
            } else {
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar colaborador\"}");
            }
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar colaborador.\"}");
        }
    }
}