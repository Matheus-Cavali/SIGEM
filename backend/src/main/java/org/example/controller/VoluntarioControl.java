package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.ConexaoSingleton;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.dao.VoluntarioDao;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.model.Voluntario;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VoluntarioControl {

    private static VoluntarioControl instancia;
    private VoluntarioControl() {}
    public static VoluntarioControl getInstancia() {
        if (instancia == null) instancia = new VoluntarioControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = org.example.util.Token.validarToken(auth.substring(7));
        }
        return token;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        boolean permitido = false;
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                UsuarioDao uDao = new UsuarioDao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) {
                        permitido = true;
                    } else {
                        RecursoSistemaDao rDao = new RecursoSistemaDao();
                        for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                            if (!permitido && r.getNome().equals(recursoNome)) {
                                permitido = true;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return permitido;
    }

    public Resposta listar(String auth, String query) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            String nome = null, email = null;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        if ("email".equalsIgnoreCase(kv[0])) email = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                List<Voluntario> lista = new VoluntarioDao().listar(conn, nome, email);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    Voluntario v = lista.get(i);
                    json.append("{");
                    json.append("\"id\":").append(v.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(v.getNome())).append("\",");
                    json.append("\"email\":\"").append(escaparJson(v.getEmail())).append("\",");
                    json.append("\"cpf\":\"").append(escaparJson(v.getCpf())).append("\",");
                    json.append("\"celular\":\"").append(escaparJson(v.getCelular())).append("\",");
                    json.append("\"statusAtivo\":").append(v.isStatusAtivo()).append(",");
                    json.append("\"dataInicio\":\"").append(v.getDataInicio() != null ? v.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "").append("\",");
                    json.append("\"dataDesligamento\":\"").append(v.getDataDesligamento() != null ? v.getDataDesligamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "").append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar voluntarios: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar voluntários.\"}");
            }
        }
        return result;
    }

    public Resposta buscarPorId(String auth, int id) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                Voluntario v = new VoluntarioDao().buscarPorId(conn, id);
                if (v == null) {
                    result = new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");
                } else {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", v.getId()); resp.addProperty("nome", v.getNome());
                    resp.addProperty("email", v.getEmail()); resp.addProperty("cpf", v.getCpf());
                    resp.addProperty("celular", v.getCelular()); resp.addProperty("statusAtivo", v.isStatusAtivo());
                    resp.addProperty("dataInicio", v.getDataInicio() != null ? v.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    resp.addProperty("dataDesligamento", v.getDataDesligamento() != null ? v.getDataDesligamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    result = new Resposta(200, resp.toString());
                }
            } catch (SQLException e) {
                System.err.println("Erro ao buscar voluntario: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao buscar voluntário.\"}");
            }
        }
        return result;
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "GESTAO_VOLUNTARIOS")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
                VoluntarioDao dao = new VoluntarioDao();
                Voluntario v = dao.buscarPorId(conn, id);
                if (v == null) {
                    result = new Resposta(404, "{\"erro\":\"Voluntário não encontrado\"}");
                } else {
                    if (body.has("nome")) v.setNome(body.get("nome").getAsString());
                    if (body.has("email")) v.setEmail(body.get("email").getAsString());
                    if (body.has("celular")) v.setCelular(body.get("celular").getAsString());
                    if (dao.atualizar(conn, v)) {
                        result = new Resposta(200, "{\"mensagem\":\"Voluntário atualizado\"}");
                    } else {
                        result = new Resposta(500, "{\"erro\":\"Erro ao atualizar voluntário\"}");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao atualizar voluntario: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar voluntário.\"}");
            }
        }
        return result;
    }

    private String escaparJson(String s) {
        String res;
        if (s == null) {
            res = "";
        } else {
            res = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
        return res;
    }
}
