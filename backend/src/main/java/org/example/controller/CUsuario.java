package org.example.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.UsuarioDao;
import org.example.model.Usuario;
import org.example.util.Criptografia;
import org.example.util.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CUsuario implements HttpHandler {

    private static CUsuario instancia;
    private CUsuario() {}
    public static CUsuario getInstancia() {
        if (instancia == null) instancia = new CUsuario();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        // O navegador envia um "OPTIONS" antes do POST para verificar permissões
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(metodo)) {
            if ("/api/login".equals(path)) {
                processarLogin(exchange);
            } else if ("/api/cadastrar".equals(path)) {
                processarCadastro(exchange);
            }
        }
    }

    private void processarLogin(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);


        Gson gson = new Gson();
        Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);


        UsuarioDao dao = new UsuarioDao();
        Usuario usuarioDoBanco = dao.buscarPorEmail(usuarioLogin.getEmail());


        if (usuarioDoBanco != null && Criptografia.verificarSenha(usuarioLogin.getSenha(), usuarioDoBanco.getSenha())) {


            String token = Token.gerarToken(usuarioDoBanco.getEmail());

            String resposta = "{\"token\":\"" + token + "\", \"mensagem\":\"Login realizado!\"}";
            enviarResposta(exchange, resposta, 200);

        } else {

            enviarResposta(exchange, "{\"erro\":\"E-mail ou senha incorretos\"}", 401);
        }
    }

    private void processarCadastro(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);


        Gson gson = new Gson();
        Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);

        String senhaBanco = Criptografia.hashSenha(usuarioLogin.getSenha());
        String cpfLimpo = usuarioLogin.getCpf().replaceAll("[^0-9]", "");
        usuarioLogin.setCpf(cpfLimpo);
        UsuarioDao dao = new UsuarioDao();
        if(dao.cadastrarUsuario(usuarioLogin.getNome(), usuarioLogin.getEmail(), senhaBanco, usuarioLogin.getCpf(), usuarioLogin.getNivelAcesso(), usuarioLogin.getTipoUsuario(), usuarioLogin.getData())){
            enviarResposta(exchange, "{\"mensagem\":\"Cadastro realizado!\"}", 201);
        }
        enviarResposta(exchange, "{\"mensagem\":\"Cadastro não recebido!\"}", 500);
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