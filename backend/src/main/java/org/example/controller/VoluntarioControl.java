package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.dao.VoluntarioDao;
import org.example.model.Resposta;
import org.example.model.Voluntario;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class VoluntarioControl {

    private static VoluntarioControl instancia;
    private VoluntarioControl() {}
    public static VoluntarioControl getInstancia() {
        if (instancia == null) instancia = new VoluntarioControl();
        return instancia;
    }

    public Resposta listar(String query) {
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
        List<Voluntario> lista = new VoluntarioDao().listar(nome, email);
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
        return new Resposta(200, json.toString());
    }

    public Resposta buscarPorId(int id) {
        Voluntario v = new VoluntarioDao().buscarPorId(id);
        if (v == null) return new Resposta(404, "{\"erro\":\"Volunt\u00e1rio n\u00e3o encontrado\"}");
        JsonObject resp = new JsonObject();
        resp.addProperty("id", v.getId()); resp.addProperty("nome", v.getNome());
        resp.addProperty("email", v.getEmail()); resp.addProperty("cpf", v.getCpf());
        resp.addProperty("celular", v.getCelular()); resp.addProperty("statusAtivo", v.isStatusAtivo());
        resp.addProperty("dataInicio", v.getDataInicio() != null ? v.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        resp.addProperty("dataDesligamento", v.getDataDesligamento() != null ? v.getDataDesligamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        return new Resposta(200, resp.toString());
    }

    public Resposta atualizar(int id, String jsonBody) {
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
        VoluntarioDao dao = new VoluntarioDao();
        Voluntario v = dao.buscarPorId(id);
        if (v == null) return new Resposta(404, "{\"erro\":\"Volunt\u00e1rio n\u00e3o encontrado\"}");
        if (body.has("nome")) v.setNome(body.get("nome").getAsString());
        if (body.has("email")) v.setEmail(body.get("email").getAsString());
        if (body.has("celular")) v.setCelular(body.get("celular").getAsString());
        if (dao.atualizar(v)) return new Resposta(200, "{\"mensagem\":\"Volunt\u00e1rio atualizado\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar\"}");
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
