package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.VoluntarioControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class VoluntarioRouter implements HttpHandler {

    private static VoluntarioRouter instancia;
    private VoluntarioRouter() {}
    public static VoluntarioRouter getInstancia() {
        if (instancia == null) instancia = new VoluntarioRouter();
        return instancia;
    }

    private VoluntarioControl controller = VoluntarioControl.getInstancia();

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

        try {
            if ("GET".equalsIgnoreCase(metodo) && "/api/voluntarios".equals(path)) {
                Resposta r = controller.listar(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);
            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
                int id = Integer.parseInt(path.split("/")[3]);
                Resposta r = controller.buscarPorId(id);
                enviarResposta(exchange, r.body, r.status);
            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/voluntarios/\\d+")) {
                int id = Integer.parseInt(path.split("/")[3]);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Resposta r = controller.atualizar(id, json);
                enviarResposta(exchange, r.body, r.status);
            } else {
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        } catch (Exception e) {
            enviarResposta(exchange, "{\"erro\":\"Erro interno\"}", 500);
        }
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, b.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
    }
}
