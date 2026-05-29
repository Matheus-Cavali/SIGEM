package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.dao.VoluntarioDao;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.model.Voluntario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

public class VoluntarioControl {

    private static VoluntarioControl instancia;
    private static Voluntario voluntario;

    private VoluntarioControl() {}

    public static VoluntarioControl getInstancia() {
        if (instancia == null) instancia = new VoluntarioControl();
        return instancia;
    }

    private final Gson gson = new Gson();

    public static synchronized Voluntario getVoluntario() {
        if (voluntario == null) voluntario = new Voluntario();
        return voluntario;
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
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public Resposta listar(String auth, String query) {
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
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
            List<Voluntario> lista = new VoluntarioDao().listar(conn, nome, email);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar voluntários.\"}");
        }
    }

    public Resposta buscarPorId(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            Voluntario v = new VoluntarioDao().buscarPorId(conn, id);
            if (v == null) return new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");
            return new Resposta(200, gson.toJson(v));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar voluntário.\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            Voluntario body = gson.fromJson(jsonBody, Voluntario.class);
            VoluntarioDao dao = new VoluntarioDao();
            Voluntario v = dao.buscarPorId(conn, id);

            if (v == null) return new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");

            if (body.getNome() != null) v.setNome(body.getNome());
            if (body.getEmail() != null) v.setEmail(body.getEmail());
            if (body.getCelular() != null) v.setCelular(body.getCelular());

            if (dao.atualizar(conn, v)) {
                return new Resposta(200, "{\"mensagem\":\"Voluntário atualizado\"}");
            } else {
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar voluntário\"}");
            }
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar voluntário.\"}");
        }
    }
}