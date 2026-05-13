package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.ColaboradorDao;
import org.example.model.Colaborador;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CColaborador implements HttpHandler {

    private static CColaborador instancia;
    private CColaborador() {}
    public static CColaborador getInstancia() {
        if (instancia == null) instancia = new CColaborador();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(metodo) && "/api/colaboradores".equals(path)) {
            listar(exchange);
        }
        else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/colaboradores/\\d+")) {
            buscarPorId(exchange);
        }
        else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/colaboradores/\\d+")) {
            atualizar(exchange);
        }
        else {
            enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
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

        ColaboradorDao dao = new ColaboradorDao();
        List<Colaborador> lista = dao.listar(nome, email);

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
        enviarResposta(exchange, json.toString(), 200);
    }

    private void buscarPorId(HttpExchange exchange) throws IOException {
        int id = extrairId(exchange.getRequestURI().getPath());
        ColaboradorDao dao = new ColaboradorDao();
        Colaborador c = dao.buscarPorId(id);

        if (c == null) {
            enviarResposta(exchange, "{\"erro\":\"Colaborador não encontrado\"}", 404);
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("id", c.getId());
        resp.addProperty("nome", c.getNome());
        resp.addProperty("email", c.getEmail());
        resp.addProperty("cpf", c.getCpf());
        resp.addProperty("celular", c.getCelular());
        resp.addProperty("nivelAcesso", c.getNivelAcesso());
        resp.addProperty("statusAtivo", c.isStatusAtivo());
        resp.addProperty("dataAdmissao", c.getDataAdmissao() != null ? c.getDataAdmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        resp.addProperty("dataDemissao", c.getDataDemissao() != null ? c.getDataDemissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        enviarResposta(exchange, resp.toString(), 200);
    }

    private void atualizar(HttpExchange exchange) throws IOException {
        int id = extrairId(exchange.getRequestURI().getPath());
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        ColaboradorDao dao = new ColaboradorDao();
        Colaborador c = dao.buscarPorId(id);
        if (c == null) {
            enviarResposta(exchange, "{\"erro\":\"Colaborador não encontrado\"}", 404);
            return;
        }

        if (body.has("nome")) c.setNome(body.get("nome").getAsString());
        if (body.has("email")) c.setEmail(body.get("email").getAsString());
        if (body.has("celular")) c.setCelular(body.get("celular").getAsString());

        if (dao.atualizar(c)) {
            enviarResposta(exchange, "{\"mensagem\":\"Colaborador atualizado\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar\"}", 500);
        }
    }

    private int extrairId(String path) {
        String[] partes = path.split("/");
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].matches("\\d+")) return Integer.parseInt(partes[i]);
        }
        return -1;
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