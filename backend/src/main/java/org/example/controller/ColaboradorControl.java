package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.Colaborador;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

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
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        String email = emailDoToken(auth);
        if (email != null) {
            UsuarioDao uDao = new UsuarioDao();
            Usuario u = uDao.buscarPorEmail(email);
            if (u != null) {
                if (u.getNivelAcesso() == 1) return true;
                RecursoSistemaDao rDao = new RecursoSistemaDao();
                for (RecursoSistema r : rDao.listarPorUsuario(u.getId())) {
                    if (r.getNome().equals(recursoNome)) return true;
                }
            }
        }
        return false;
    }

    public Resposta listar(String auth, String query) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
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
        List<Colaborador> lista = new ColaboradorDao().listar(nome, email);
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
        return new Resposta(200, json.toString());
    }

    public Resposta buscarPorId(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Colaborador c = new ColaboradorDao().buscarPorId(id);
        if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");
        JsonObject resp = new JsonObject();
        resp.addProperty("id", c.getId()); resp.addProperty("nome", c.getNome());
        resp.addProperty("email", c.getEmail()); resp.addProperty("cpf", c.getCpf());
        resp.addProperty("celular", c.getCelular()); resp.addProperty("nivelAcesso", c.getNivelAcesso());
        resp.addProperty("statusAtivo", c.isStatusAtivo());
        resp.addProperty("dataAdmissao", c.getDataAdmissao() != null ? c.getDataAdmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        resp.addProperty("dataDemissao", c.getDataDemissao() != null ? c.getDataDemissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        return new Resposta(200, resp.toString());
    }

    public Resposta atualizar(String auth, int id, String jsonBody) {
        if (!usuarioTemPermissao(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
        ColaboradorDao dao = new ColaboradorDao();
        Colaborador c = dao.buscarPorId(id);
        if (c == null) return new Resposta(404, "{\"erro\":\"Colaborador não encontrado\"}");
        if (body.has("nome")) c.setNome(body.get("nome").getAsString());
        if (body.has("email")) c.setEmail(body.get("email").getAsString());
        if (body.has("celular")) c.setCelular(body.get("celular").getAsString());
        if (dao.atualizar(c)) return new Resposta(200, "{\"mensagem\":\"Colaborador atualizado\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar\"}");
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
