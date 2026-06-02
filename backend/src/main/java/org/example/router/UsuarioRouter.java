package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.UsuarioControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UsuarioRouter implements HttpHandler {

    private UsuarioControl control;

    public UsuarioRouter() {
        control = new UsuarioControl();
    }

    public UsuarioControl getControl() {
        return control;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            String path = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");

            try {
                if ("POST".equalsIgnoreCase(metodo)) {
                    if ("/api/login".equals(path)) {
                        processarLogin(exchange);
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
                } else if ("GET".equalsIgnoreCase(metodo) && "/api/recurso/usuarios".equals(path)) {
                    listarUsuariosPorPermissao(exchange, auth);
                } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+")) {
                    removerUsuario(exchange);
                } else {
                    enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
                }
            } catch (Exception e) {
                System.err.println("ERRO: " + e.getMessage());
                enviarResposta(exchange, "{\"erro\":\"Erro interno do servidor\"}", 500);
            }
        }
    }

    private void processarLogin(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = control.processarLogin(json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void processarAlteracaoSenha(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = control.processarAlteracaoSenha(json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void processarCadastroInterno(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        Resposta r = control.processarCadastroInterno(json, auth);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarUsuarios(HttpExchange exchange, String auth) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Resposta r = control.listarUsuarios(auth, query);
        enviarResposta(exchange, r.body, r.status);
    }

    private void buscarUsuario(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        Resposta r = control.buscarUsuario(auth, id);
        enviarResposta(exchange, r.body, r.status);
    }

    private void alterarStatusUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = control.alterarStatusUsuario(auth, id, json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void atualizarUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = control.atualizarUsuario(auth, id, json);
        enviarResposta(exchange, r.body, r.status);
    }

    private void removerUsuario(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String[] p = exchange.getRequestURI().getPath().split("/");
        int id = Integer.parseInt(p[3]);
        Resposta r = control.removerUsuario(auth, id);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarTodosRecursos(HttpExchange exchange, String auth) throws IOException {
        Resposta r = control.listarTodosRecursos(auth);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarUsuariosPorPermissao(HttpExchange exchange, String auth) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String permissao = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2 && "permissao".equalsIgnoreCase(kv[0]))
                    permissao = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        Resposta r = control.listarUsuariosPorPermissao(auth, permissao);
        enviarResposta(exchange, r.body, r.status);
    }

    private void listarPermissoes(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(p[3]);
        Resposta r = control.listarPermissoes(auth, usuarioId);
        enviarResposta(exchange, r.body, r.status);
    }

    private void atualizarPermissoes(HttpExchange exchange, String auth) throws IOException {
        String[] p = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(p[3]);
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Resposta r = control.atualizarPermissoes(auth, usuarioId, json);
        enviarResposta(exchange, r.body, r.status);
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
