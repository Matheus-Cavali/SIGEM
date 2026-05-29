package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.InvestimentoControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class InvestimentoRouter implements HttpHandler {

    private InvestimentoControl control;

    public InvestimentoRouter() {
        control = new InvestimentoControl();
    }

    public InvestimentoControl getControl() {
        return control;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            String path = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");

            try {
                Resposta r;

                if ("POST".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = control.registrarInvestimento(auth, json);
                } else if ("GET".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                    r = control.listarInvestimentos(auth, exchange.getRequestURI().getQuery());
                } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    r = control.buscarInvestimento(auth, id);
                } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = control.atualizarInvestimento(auth, id, json);
                } else if ("GET".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                    int id = extrairId(path);
                    r = control.listarAportes(auth, id);
                } else if ("POST".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                    int id = extrairId(path);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = control.lancarAporte(auth, id, json);
                } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    r = control.removerInvestimento(auth, id);
                } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                    String[] p = path.split("/");
                    int aporteId = Integer.parseInt(p[p.length - 1]);
                    r = control.removerAporte(auth, aporteId);
                } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                    String[] p = path.split("/");
                    int aporteId = Integer.parseInt(p[p.length - 1]);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = control.atualizarAporte(auth, aporteId, json);
                } else {
                    r = new Resposta(404, "{\"erro\":\"Rota não encontrada\"}");
                }

                enviarResposta(exchange, r.body, r.status);

            } catch (Exception e) {
                System.err.println("ERRO: " + e.getMessage());
                enviarResposta(exchange, "{\"erro\":\"Erro interno do servidor\"}", 500);
            }
        }
    }

    private int extrairId(String path) {
        String[] p = path.split("/");
        for (int i = 0; i < p.length; i++) {
            if (p[i].matches("\\d+")) return Integer.parseInt(p[i]);
        }
        return -1;
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("Erro crítico de I/O: " + e.getMessage());
        }
    }
}
