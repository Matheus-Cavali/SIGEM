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
    public static synchronized DespesaRouter getInstancia() {
        if (instancia == null) instancia = new DespesaRouter();
        return instancia;
    }

    private final DespesaControl controller = DespesaControl.getInstancia();

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

            // ── Categorias de Despesa ─────────────────────────────────────────

            if ("POST".equalsIgnoreCase(metodo) && "/api/categorias-despesa".equals(path)) {
                Resposta r = controller.criarCategoriaDespesa(auth, lerBody(exchange));
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/categorias-despesa".equals(path)) {
                Resposta r = controller.listarCategoriasDespesa(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/categorias-despesa/\\d+")) {
                Resposta r = controller.atualizarCategoriaDespesa(auth, extrairUltimoId(path), lerBody(exchange));
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/categorias-despesa/\\d+")) {
                Resposta r = controller.deletarCategoriaDespesa(auth, extrairUltimoId(path));
                enviarResposta(exchange, r.body, r.status);

            // ── Saldo (deve vir antes de /api/despesas/\d+ para não ser capturado) ──

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/despesas/saldo".equals(path)) {
                Resposta r = controller.consultarSaldo();
                enviarResposta(exchange, r.body, r.status);

            // ── Despesas ──────────────────────────────────────────────────────

            } else if ("POST".equalsIgnoreCase(metodo) && "/api/despesas".equals(path)) {
                Resposta r = controller.lancarDespesa(auth, lerBody(exchange));
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/despesas".equals(path)) {
                Resposta r = controller.listarDespesas(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                Resposta r = controller.buscarDespesa(extrairUltimoId(path));
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                Resposta r = controller.atualizarDespesa(auth, extrairUltimoId(path), lerBody(exchange));
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+")) {
                Resposta r = controller.deletarDespesa(auth, extrairUltimoId(path));
                enviarResposta(exchange, r.body, r.status);

            } else if ("POST".equalsIgnoreCase(metodo) && path.matches("/api/despesas/\\d+/quitar")) {
                Resposta r = controller.quitarDespesa(auth, extrairUltimoId(path), lerBody(exchange));
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

    private int extrairUltimoId(String path) {
        String[] partes = path.split("/");
        for (int i = partes.length - 1; i >= 0; i--) {
            if (partes[i].matches("\\d+")) return Integer.parseInt(partes[i]);
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
