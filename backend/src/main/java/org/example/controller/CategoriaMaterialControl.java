package org.example.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.example.facade.CategoriaMaterialFacade;
import org.example.model.CategoriaMaterial;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CategoriaMaterialControl{
    private static final CategoriaMaterialControl instancia = new CategoriaMaterialControl();
    private final CategoriaMaterialFacade facade = CategoriaMaterialFacade.getInstancia();
    private final Gson gson = new Gson();

    private CategoriaMaterialControl(){}
    public static CategoriaMaterialControl getInstancia(){
        return instancia;
    }

    public void cadastrar(HttpExchange exchange){
        try{
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CategoriaMaterial cm = gson.fromJson(json, CategoriaMaterial.class);
            facade.cadastrar(cm);
            enviarResposta(exchange, "{\"mensagem\":\"Categoria de material cadastrada com sucesso\"}", 201);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Falha ao cadastrar categoria de material\"}", 400);
        }
    }

    public void listar(HttpExchange exchange){
        try{
            String query = exchange.getRequestURI().getQuery();
            String nome = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2 && "nome".equalsIgnoreCase(pair[0])){
                        nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }
            List<CategoriaMaterial> lista = (nome != null && !nome.trim().isEmpty()) ? facade.buscarPorNome(nome) : facade.listarTodos();
            enviarResposta(exchange, gson.toJson(lista), 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao listar categorias de material\"}", 500);
        }
    }

    public void atualizar(HttpExchange exchange){
        try{
            int id = extrairId(exchange.getRequestURI().getPath());
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CategoriaMaterial cm = gson.fromJson(json, CategoriaMaterial.class);

            cm.setId(id);
            facade.alterar(cm);
            enviarResposta(exchange, "{\"mensagem\":\"Categoria de material atualizada com sucesso\"}", 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar categoria de material\"}", 400);
        }
    }

    public void excluir(HttpExchange exchange){
        try{
            int id = extrairId(exchange.getRequestURI().getPath());

            facade.excluir(id);
            enviarResposta(exchange, "{\"mensagem\":\"Categoria de material excluída com sucesso\"}", 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao excluir categoria de material\"}", 400);
        }
    }

    private int extrairId(String path){
        try{
            return Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
        }
        catch (NumberFormatException e){
            return -1;
        }
    }

    private void enviarResposta(HttpExchange exchange, String json, int status){
        try{
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try(OutputStream os = exchange.getResponseBody()){
                os.write(bytes);
            }
        }
        catch (IOException e){
            System.err.println("Erro crítico de I/O: " + e.getMessage());
        }
    }
}