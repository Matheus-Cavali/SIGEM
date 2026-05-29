package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.EventoControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EventoRouter implements HttpHandler {

    private static EventoControl control;

    public static synchronized EventoControl getControl() {

        if (control == null)
            control = new EventoControl();

        return control;
    }

    public EventoRouter() {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods",
                "POST, GET, PUT, DELETE, PATCH, OPTIONS");

        exchange.getResponseHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, Authorization");

        String metodo = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        if ("OPTIONS".equalsIgnoreCase(metodo)) {

            exchange.sendResponseHeaders(204, -1);
        } else try {

            Resposta r;

            if ("POST".equalsIgnoreCase(metodo)
                    && "/api/eventos".equals(path)) {

                String json = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                r = getControl().cadastrar(auth, json);
            }

            else if ("GET".equalsIgnoreCase(metodo)
                    && "/api/eventos".equals(path)) {

                r = getControl().listar(
                        exchange.getRequestURI().getQuery()
                );
            }

            else if ("PUT".equalsIgnoreCase(metodo)
                    && path.matches("/api/eventos/\\d+")) {

                int id = Integer.parseInt(
                        path.substring(path.lastIndexOf("/") + 1)
                );

                String json = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                r = getControl().atualizar(auth, id, json);
            }

            else if ("PATCH".equalsIgnoreCase(metodo)
                    && path.matches("/api/eventos/\\d+/abrir")) {

                String[] partes = path.split("/");

                int id = Integer.parseInt(partes[3]);

                r = getControl().abrir(auth, id);
            }

            else if ("PATCH".equalsIgnoreCase(metodo)
                    && path.matches("/api/eventos/\\d+/cancelar")) {

                String[] partes = path.split("/");

                int id = Integer.parseInt(partes[3]);

                r = getControl().cancelar(auth, id);
            }

            else if ("PATCH".equalsIgnoreCase(metodo)
                    && path.matches("/api/eventos/\\d+/encerrar")) {

                String[] partes = path.split("/");

                int id = Integer.parseInt(partes[3]);

                String json = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                r = getControl().encerrar(auth, id, json);
            }

            else if ("DELETE".equalsIgnoreCase(metodo)
                    && path.matches("/api/eventos/\\d+")) {

                int id = Integer.parseInt(
                        path.substring(path.lastIndexOf("/") + 1)
                );

                r = getControl().excluir(auth, id);
            }

            else {

                r = new Resposta(404,
                        "{\"erro\":\"Rota não encontrada\"}");
            }

            enviarResposta(exchange, r.body, r.status);
        }
        catch (Exception e) {

            System.err.println("ERRO: " + e.getMessage());

            enviarResposta(exchange,
                    "{\"erro\":\"Erro interno do servidor\"}",
                    500);
        }
    }

    private void enviarResposta(HttpExchange exchange,
                                String json,
                                int status) {

        try {

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set("Content-Type", "application/json");

            exchange.sendResponseHeaders(status, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {

                os.write(bytes);
            }
        }
        catch (IOException e) {

            System.err.println(
                    "Erro crítico de I/O: " + e.getMessage()
            );
        }
    }
}