package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.VoluntarioControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class VoluntarioRouter implements HttpHandler {

    private VoluntarioControl control;

    public VoluntarioRouter() {
        control = new VoluntarioControl();
    }

    public VoluntarioControl getControl() {
        return control;
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
            String auth = exchange.getRequestHeaders().getFirst("Authorization");

            try {
                Resposta r;

                if ("GET".equalsIgnoreCase(metodo) && "/api/voluntarios".equals(path)) {
                    r = controller.listar(auth, exchange.getRequestURI().getQuery());
                } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
                    int id = Integer.parseInt(path.split("/")[3]);
                    r = controller.buscarPorId(auth, id);
                } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
                    int id = Integer.parseInt(path.split("/")[3]);
                    String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    r = controller.atualizar(auth, id, json);
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
