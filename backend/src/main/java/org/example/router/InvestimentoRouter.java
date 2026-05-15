package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.InvestimentoControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class InvestimentoRouter implements HttpHandler {

    private static InvestimentoRouter instancia;
    private InvestimentoRouter() {}
    public static InvestimentoRouter getInstancia() {
        if (instancia == null) instancia = new InvestimentoRouter();
        return instancia;
    }

    private InvestimentoControl controller = InvestimentoControl.getInstancia();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        try {
            if ("POST".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Resposta r = controller.registrarInvestimento(auth, json);
                enviarResposta(exchange, r.body, r.status);
            } else if ("GET".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                Resposta r = controller.listarInvestimentos(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);
            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                int id = extrairId(path);
                Resposta r = controller.buscarInvestimento(id);
                enviarResposta(exchange, r.body, r.status);
            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                int id = extrairId(path);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Resposta r = controller.atualizarInvestimento(auth, id, json);
                enviarResposta(exchange, r.body, r.status);
            } else if ("GET".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                int id = extrairId(path);
                Resposta r = controller.listarAportes(id);
                enviarResposta(exchange, r.body, r.status);
            } else if ("POST".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                int id = extrairId(path);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Resposta r = controller.lancarAporte(auth, id, json);
                enviarResposta(exchange, r.body, r.status);
            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                int id = extrairId(path);
                Resposta r = controller.removerInvestimento(auth, id);
                enviarResposta(exchange, r.body, r.status);
            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                String[] p = path.split("/");
                int aporteId = Integer.parseInt(p[p.length - 1]);
                Resposta r = controller.removerAporte(auth, aporteId);
                enviarResposta(exchange, r.body, r.status);
            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                String[] p = path.split("/");
                int aporteId = Integer.parseInt(p[p.length - 1]);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Resposta r = controller.atualizarAporte(auth, aporteId, json);
                enviarResposta(exchange, r.body, r.status);
            } else {
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
            enviarResposta(exchange, "{\"erro\":\"Erro interno\"}", 500);
        }
    }

    private int extrairId(String path) {
        String[] p = path.split("/");
        for (int i = 0; i < p.length; i++) {
            if (p[i].matches("\\d+")) return Integer.parseInt(p[i]);
        }
        return -1;
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] respostaBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, respostaBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(respostaBytes); }
    }
}
