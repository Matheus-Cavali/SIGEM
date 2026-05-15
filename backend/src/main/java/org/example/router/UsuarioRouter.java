package org.example.router;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.UsuarioControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UsuarioRouter implements HttpHandler {

    private static UsuarioRouter instancia;
    private UsuarioRouter() {}
    public static UsuarioRouter getInstancia() {
        if (instancia == null) instancia = new UsuarioRouter();
        return instancia;
    }

    private UsuarioControl controller = UsuarioControl.getInstancia();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        try {
            if ("POST".equalsIgnoreCase(metodo)) {
                if ("/api/login".equals(path)) {
                    processarLogin(exchange);
                } else if ("/api/cadastrar".equals(path)) {
                    processarCadastro(exchange);
                } else if ("/api/alterar-Primeira-Senha".equals(path)) {
                    processarAlteracaoSenha(exchange);
                } else if ("/api/cadastrar-interno".equals(path)) {
                    processarCadastroInterno(exchange);
                }
            } else if ("GET".equalsIgnoreCase(metodo) && "/api/usuarios".equals(path)) {
                listarUsuarios(exchange, auth);
            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+")) {
                buscarUsuario(exchange, auth);
            } else if ("PATCH".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+/status")) {
                alterarStatusUsuario(exchange);
            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+")) {
                atualizarUsuario(exchange);
            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/recurso/\\d+/permissoes")) {
                listarPermissoes(exchange, auth);
            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/recurso/\\d+/permissoes")) {
                atualizarPermissoes(exchange, auth);
            } else if ("GET".equalsIgnoreCase(metodo) && "/api/recurso".equals(path)) {
                listarTodosRecursos(exchange, auth);
            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+")) {
                removerUsuario(exchange);
            }
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
            enviarResposta(exchange, "{\"erro\":\"Erro interno\"}", 500);
        }
    }

    private void processarLogin(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.processarLogin(json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void processarCadastro(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.processarCadastro(json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void processarAlteracaoSenha(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.processarAlteracaoSenha(json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void processarCadastroInterno(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        Resposta r = controller.processarCadastroInterno(json, auth);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarUsuarios(HttpExchange exchange, String auth) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Resposta r = controller.listarUsuarios(auth, query);
        enviarResposta(exchange, r.body, r.status);
    }

    private void buscarUsuario(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        Resposta r = controller.buscarUsuario(auth, id);
        enviarResposta(exchange, r.body, r.status);
    }

    private void alterarStatusUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String path = exchange.getRequestURI().getPath();
        String[] p = path.split("/");
        int id = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.alterarStatusUsuario(auth, id, json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void atualizarUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.atualizarUsuario(auth, id, json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void removerUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        Resposta r = controller.removerUsuario(auth, id);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarTodosRecursos(HttpExchange exchange, String auth) throws IOException {
        Resposta r = controller.listarTodosRecursos(auth);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarPermissoes(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(p[3]);
        Resposta r = controller.listarPermissoes(auth, usuarioId);
        enviarResposta(exchange, r.body, r.status);
    }

    private void atualizarPermissoes(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = controller.atualizarPermissoes(auth, usuarioId, json);
        enviarResposta(exchange, r.body, r.status);
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
