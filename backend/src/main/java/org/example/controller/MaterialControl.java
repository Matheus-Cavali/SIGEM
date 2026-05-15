package org.example.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.facade.MaterialFacade;
import org.example.model.Material;
import org.example.model.RecursoSistema;
import org.example.model.Usuario;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MaterialControl {
    private static final MaterialControl instancia = new MaterialControl();
    private final MaterialFacade facade = MaterialFacade.getInstancia();
    private final Gson gson = new Gson();

    private MaterialControl() {}

    public static MaterialControl getInstancia(){
        return instancia;
    }

    public void cadastrar(HttpExchange exchange){
        try{
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Material m = gson.fromJson(json, Material.class);
            facade.cadastrar(m);
            enviarResposta(exchange, "{\"mensagem\":\"Material cadastrado com sucesso\"}", 201);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Falha ao cadastrar material\"}", 400);
        }
    }

    public void listar(HttpExchange exchange){
        try{
            String query = exchange.getRequestURI().getQuery();
            String nome = null;
            Integer categoriaId = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2){
                        if("nome".equalsIgnoreCase(pair[0])){
                            nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        }
                        else if("categoriaId".equalsIgnoreCase(pair[0])){
                            categoriaId = Integer.parseInt(pair[1]);
                        }
                    }
                }
            }

            List<Material> lista = facade.filtrar(nome, categoriaId);
            enviarResposta(exchange, gson.toJson(lista), 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao listar materiais\"}", 500);
        }
    }

    public void atualizar(HttpExchange exchange){
        try{
            int id = extrairId(exchange.getRequestURI().getPath());
            String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            Material m = gson.fromJson(json, Material.class);
            m.setId(id);

            facade.alterar(m);
            enviarResposta(exchange, "{\"mensagem\":\"Material atualizado com sucesso\"}", 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar material\"}", 400);
        }
    }

    public void excluir(HttpExchange exchange){
        try{
            int id = extrairId(exchange.getRequestURI().getPath());

            facade.excluir(id);
            enviarResposta(exchange, "{\"mensagem\":\"Material excluído com sucesso\"}", 200);
        }
        catch (Exception e){
            enviarResposta(exchange, "{\"erro\":\"Erro ao excluir material\"}", 400);
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
