package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.VoluntarioDao;
import org.example.model.Voluntario;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CVoluntario implements HttpHandler {

    private static CVoluntario instancia;
    private CVoluntario() {}
    public static CVoluntario getInstancia() {
        if (instancia == null) instancia = new CVoluntario();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            String path = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(metodo) && "/api/voluntarios".equals(path)) {
            listar(exchange);
        }
        else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
            buscarPorId(exchange);
        }
        else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
            atualizar(exchange);
        }
        else {
            enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
        }
            }
    }

    private void listar(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String nome = null, email = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if ("email".equalsIgnoreCase(kv[0])) email = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        VoluntarioDao dao = new VoluntarioDao();
        List<Voluntario> lista = dao.listar(nome, email);

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
        enviarResposta(exchange, json.toString(), 200);
    }

    private void buscarPorId(HttpExchange exchange) throws IOException {
        int id = extrairId(exchange.getRequestURI().getPath());
        VoluntarioDao dao = new VoluntarioDao();
        Voluntario v = dao.buscarPorId(id);

        if (v == null) {
            enviarResposta(exchange, "{\"erro\":\"Voluntário não encontrado\"}", 404);
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("id", v.getId());
        resp.addProperty("nome", v.getNome());
        resp.addProperty("email", v.getEmail());
        resp.addProperty("cpf", v.getCpf());
        resp.addProperty("celular", v.getCelular());
        resp.addProperty("statusAtivo", v.isStatusAtivo());
        resp.addProperty("dataInicio", v.getDataInicio() != null ? v.getDataInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        resp.addProperty("dataDesligamento", v.getDataDesligamento() != null ? v.getDataDesligamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        enviarResposta(exchange, resp.toString(), 200);
    }

    private void atualizar(HttpExchange exchange) throws IOException {
        int id = extrairId(exchange.getRequestURI().getPath());
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        VoluntarioDao dao = new VoluntarioDao();
        Voluntario v = dao.buscarPorId(id);
        if (v == null) {
            enviarResposta(exchange, "{\"erro\":\"Voluntário não encontrado\"}", 404);
            return;
        }

        if (body.has("nome")) v.setNome(body.get("nome").getAsString());
        if (body.has("email")) v.setEmail(body.get("email").getAsString());
        if (body.has("celular")) v.setCelular(body.get("celular").getAsString());

        if (dao.atualizar(v)) {
            enviarResposta(exchange, "{\"mensagem\":\"Voluntário atualizado\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar\"}", 500);
        }
    }

    private int extrairId(String path) {
        int resultado = -1;
        String[] partes = path.split("/");
        for (int i = 0; i < partes.length && resultado == -1; i++) {
            if (partes[i].matches("\\d+")) {
                resultado = Integer.parseInt(partes[i]);
            }
        }
        return resultado;
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] respostaBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, respostaBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respostaBytes);
        }
    }
}