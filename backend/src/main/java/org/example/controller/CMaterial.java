package org.example.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.facade.MaterialFacade;
import org.example.model.Material;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CMaterial implements HttpHandler {
    private static CMaterial instancia;
    private final MaterialFacade facade = new MaterialFacade();
    private final Gson gson = new Gson();

    private CMaterial() {}

    public static CMaterial getInstancia(){
        if(instancia == null)
            instancia = new CMaterial();

        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException{
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String metodo = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try{
            if("OPTIONS".equalsIgnoreCase(metodo)){
                exchange.sendResponseHeaders(204, -1);
            }
            else if("POST".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
                cadastrar(exchange);
            }
            else if("GET".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
                listar(exchange);
            }
            else if("PUT".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
                atualizar(exchange);
            }
            else if("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
                excluir(exchange);
            }
            else{
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        }
        catch (IllegalArgumentException e){
            enviarResposta(exchange, "{\"erro\":\"" + e.getMessage() + "\"}", 400);
        }
        catch (Exception e){
            String msg = (e.getMessage() != null) ? e.getMessage() : "Erro interno no servidor";
            enviarResposta(exchange, "{\"erro\":\"" + msg + "\"}", 500);
        }
    }

    private void cadastrar(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Material m = gson.fromJson(json, Material.class);

        facade.cadastrar(m);
        enviarResposta(exchange, "{\"mensagem\":\"Material cadastrado com sucesso\"}", 201);
    }

    private void listar(HttpExchange exchange) throws IOException{
        String query = exchange.getRequestURI().getQuery();
        String nome = null;
        Integer categoriaId = null;

        if(query != null){
            for(String p : query.split("&")){
                String[] kv = p.split("=", 2);

                if(kv.length == 2){
                    if("nome".equalsIgnoreCase(kv[0]))
                        nome = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if("categoriaId".equalsIgnoreCase(kv[0]))
                        categoriaId = Integer.parseInt(kv[1]);
                }
            }
        }

        List<Material> lista = facade.filtrar(nome, categoriaId);
        enviarResposta(exchange, gson.toJson(lista), 200);
    }

    private void atualizar(HttpExchange exchange) throws IOException{
        int id = extrairId(exchange.getRequestURI().getPath());
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);

        Material m = gson.fromJson(json, Material.class);
        m.setId(id);

        facade.alterar(m);
        enviarResposta(exchange, "{\"mensagem\":\"Material atualizado com sucesso\"}", 200);
    }

    private void excluir(HttpExchange exchange) throws IOException{
        int id = extrairId(exchange.getRequestURI().getPath());

        facade.excluir(id);
        enviarResposta(exchange, "{\"mensagem\":\"Material excluído com sucesso\"}", 200);
    }

    private int extrairId(String path){
        String[] partes = path.split("/");

        return Integer.parseInt(partes[partes.length - 1]);
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException{
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try(OutputStream os = exchange.getResponseBody()){
            os.write(bytes);
        }
    }
}