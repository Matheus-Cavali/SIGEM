package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.DespesaControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DespesaRouter implements HttpHandler {

    private static DespesaRouter instancia;
    private DespesaRouter() {}
    public static DespesaRouter getInstancia() {
        if (instancia == null) instancia = new DespesaRouter();
        return instancia;
    }

    private DespesaControl controller = DespesaControl.getInstancia();

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

            // ── Categorias de Despesa: /api/categorias-despesa ────────────────

            if ("POST".equalsIgnoreCase(metodo) && "/api/categorias-despesa".equals(path)) {
                String json = lerBody(exchange);
                Resposta r = controller.criarCategoriaDespesa(auth, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/categorias-despesa".equals(path)) {
                Resposta r = controller.listarCategoriasDespesa(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/categorias-despesa/\\d+")) {
                int id = extrairUltimoId(path);
                String json = lerBody(exchange);
                Resposta r = controller.atualizarCategoriaDespesa(auth, id, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/categorias-despesa/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.deletarCategoriaDespesa(auth, id);
                enviarResposta(exchange, r.body, r.status);

                // ── Despesas: /api/despesas ────────────────────────────────────────

            } else if ("POST".equalsIgnoreCase(metodo) && "/api/despesas".equals(path)) {
                String json = lerBody(exchange);
                Resposta r = controller.lancarDespesa(auth, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/despesas".equals(path)) {
                Resposta r = controller.listarDespesas(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.buscarDespesa(id);
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                int id = extrairUltimoId(path);
                String json = lerBody(exchange);
                Resposta r = controller.atualizarDespesa(auth, id, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.deletarDespesa(auth, id);
                enviarResposta(exchange, r.body, r.status);

            } else {
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }

        } catch (Exception e) {
            System.err.println("ERRO DespesaRouter: " + e.getMessage());
            e.printStackTrace();
            enviarResposta(exchange, "{\"erro\":\"Erro interno\"}", 500);
        }
    }

    private String lerBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Extrai o último número do path (útil para sub-recursos). */
    private int extrairUltimoId(String path) {
        String[] partes = path.split("/");
        for (int i = partes.length - 1; i >= 0; i--) {
            if (partes[i].matches("\\d+")) return Integer.parseInt(partes[i]);
        }
        return -1;
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
