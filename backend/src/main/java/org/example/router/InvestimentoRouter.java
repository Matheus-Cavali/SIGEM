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
        } else {
            String path = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");

            try {
                Resposta r;

                if ("POST".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = controller.registrarInvestimento(auth, json);
                } else if ("GET".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                    r = controller.listarInvestimentos(auth, exchange.getRequestURI().getQuery());
                } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    r = controller.buscarInvestimento(auth, id);
                } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = controller.atualizarInvestimento(auth, id, json);
                } else if ("GET".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                    int id = extrairId(path);
                    r = controller.listarAportes(auth, id);
                } else if ("POST".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                    int id = extrairId(path);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = controller.lancarAporte(auth, id, json);
                } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                    int id = extrairId(path);
                    r = controller.removerInvestimento(auth, id);
                } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                    String[] p = path.split("/");
                    int aporteId = Integer.parseInt(p[p.length - 1]);
                    r = controller.removerAporte(auth, aporteId);
                } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                    String[] p = path.split("/");
                    int aporteId = Integer.parseInt(p[p.length - 1]);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = controller.atualizarAporte(auth, aporteId, json);
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