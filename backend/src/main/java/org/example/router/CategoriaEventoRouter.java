package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.CategoriaEventoControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CategoriaEventoRouter implements HttpHandler {

    private static CategoriaEventoRouter instancia;

    private CategoriaEventoRouter() {}

    public static CategoriaEventoRouter getInstancia() {

        if (instancia == null) {
            instancia = new CategoriaEventoRouter();
        }

        return instancia;
    }

    private CategoriaEventoControl controller =
            CategoriaEventoControl.getInstancia();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Origin",
                "*"
        );

        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Methods",
                "POST, GET, PUT, DELETE, OPTIONS"
        );

        exchange.getResponseHeaders().add(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization"
        );

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {

            exchange.sendResponseHeaders(204, -1);

            return;
        }

        String path = exchange.getRequestURI().getPath();

        String metodo = exchange.getRequestMethod();

        String auth =
                exchange.getRequestHeaders().getFirst("Authorization");

        try {

            if (
                    "POST".equalsIgnoreCase(metodo) &&
                            "/api/categorias-eventos".equals(path)
            ) {

                String json = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                Resposta r =
                        controller.cadastrar(auth, json);

                enviarResposta(exchange, r.body, r.status);
            }

            else if (
                    "GET".equalsIgnoreCase(metodo) &&
                            "/api/categorias-eventos".equals(path)
            ) {

                Resposta r =
                        controller.listar(
                                auth,
                                exchange.getRequestURI().getQuery()
                        );

                enviarResposta(exchange, r.body, r.status);
            }

            else if (
                    "PUT".equalsIgnoreCase(metodo) &&
                            path.matches("/api/categorias-eventos/\\d+")
            ) {

                int id = extrairId(path);

                String json = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                Resposta r =
                        controller.atualizar(auth, id, json);

                enviarResposta(exchange, r.body, r.status);
            }

            else if (
                    "DELETE".equalsIgnoreCase(metodo) &&
                            path.matches("/api/categorias-eventos/\\d+")
            ) {

                int id = extrairId(path);

                Resposta r =
                        controller.excluir(auth, id);

                enviarResposta(exchange, r.body, r.status);
            }

            else {

                enviarResposta(
                        exchange,
                        "{\"erro\":\"Rota não encontrada\"}",
                        404
                );
            }

        } catch (Exception e) {

            System.err.println("ERRO: " + e.getMessage());

            enviarResposta(
                    exchange,
                    "{\"erro\":\"Erro interno\"}",
                    500
            );
        }
    }

    private int extrairId(String path) {

        String[] partes = path.split("/");

        for (int i = 0; i < partes.length; i++) {

            if (partes[i].matches("\\d+")) {

                return Integer.parseInt(partes[i]);
            }
        }

        return -1;
    }

    private void enviarResposta(
            HttpExchange exchange,
            String json,
            int status
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json"
        );

        byte[] respostaBytes =
                json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                respostaBytes.length
        );

        try (OutputStream os = exchange.getResponseBody()) {

            os.write(respostaBytes);
        }
    }
}