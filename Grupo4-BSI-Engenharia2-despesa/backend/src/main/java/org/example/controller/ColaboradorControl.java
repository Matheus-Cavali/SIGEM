package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.Colaborador;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ColaboradorControl {

    private static ColaboradorControl instancia;
    private ColaboradorControl() {}
    public static ColaboradorControl getInstancia() {
        if (instancia == null) instancia = new ColaboradorControl();
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
            try (Connection conn = Conexao.getConexao()) {
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
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
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
            try (Connection conn = Conexao.getConexao()) {
                List<Colaborador> lista = new ColaboradorDao().listar(conn, nome, email);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    Colaborador c = lista.get(i);
                    json.append("{");
                    json.append("\"id\":").append(c.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(c.getNome())).append("\",");
                    json.append("\"email\":\"").append(escaparJson(c.getEmail())).append("\",");
                    json.append("\"cpf\":\"").append(escaparJson(c.getCpf())).append("\",");
                    json.append("\"celular\":\"").append(escaparJson(c.getCelular())).append("\",");
                    json.append("\"nivelAcesso\":").append(c.getNivelAcesso()).append(",");
                    json.append("\"statusAtivo\":").append(c.isStatusAtivo()).append(",");
                    json.append("\"dataAdmissao\":\"").append(c.getDataAdmissao() != null ? c.getDataAdmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "").append("\",");
                    json.append("\"dataDemissao\":\"").append(c.getDataDemissao() != null ? c.getDataDemissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "").append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar colaboradores: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar colaboradores.\"}");
            }
        }
        return result;
    }

    public Resposta buscarPorId(String auth, int id) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = Conexao.getConexao()) {
                Colaborador c = new ColaboradorDao().buscarPorId(conn, id);
                if (c == null) {
                    result = new Resposta(404, "{\"erro\":\"Colaborador nÃ£o encontrado\"}");
                } else {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", c.getId()); resp.addProperty("nome", c.getNome());
                    resp.addProperty("email", c.getEmail()); resp.addProperty("cpf", c.getCpf());
                    resp.addProperty("celular", c.getCelular()); resp.addProperty("nivelAcesso", c.getNivelAcesso());
                    resp.addProperty("statusAtivo", c.isStatusAtivo());
                    resp.addProperty("dataAdmissao", c.getDataAdmissao() != null ? c.getDataAdmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    resp.addProperty("dataDemissao", c.getDataDemissao() != null ? c.getDataDemissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    result = new Resposta(200, resp.toString());
                }
            } catch (SQLException e) {
                System.err.println("Erro ao buscar colaborador: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao buscar colaborador.\"}");
            }
        }
        return result;
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = Conexao.getConexao()) {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
                ColaboradorDao dao = new ColaboradorDao();
                Colaborador c = dao.buscarPorId(conn, id);
                if (c == null) {
                    result = new Resposta(404, "{\"erro\":\"Colaborador nÃ£o encontrado\"}");
                } else {
                    if (body.has("nome")) c.setNome(body.get("nome").getAsString());
                    if (body.has("email")) c.setEmail(body.get("email").getAsString());
                    if (body.has("celular")) c.setCelular(body.get("celular").getAsString());
                    if (dao.atualizar(conn, c)) {
                        result = new Resposta(200, "{\"mensagem\":\"Colaborador atualizado\"}");
                    } else {
                        result = new Resposta(500, "{\"erro\":\"Erro ao atualizar colaborador\"}");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao atualizar colaborador: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar colaborador.\"}");
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
