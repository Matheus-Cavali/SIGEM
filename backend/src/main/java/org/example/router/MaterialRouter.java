package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import org.example.controller.MaterialControl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MaterialRouter implements HttpHandler {
    private static final MaterialRouter instancia = new MaterialRouter();

    private MaterialRouter() {}

    public static MaterialRouter getInstancia(){
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange){
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String metodo = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if("OPTIONS".equalsIgnoreCase(metodo)){
            enviarRespostaVazia(exchange, 204);
        }
        else{
            try{
                processarRota(exchange, metodo, path);
            }
            catch (Exception e){
                System.err.println("ERRO: " + e.getMessage());
                int status = (e instanceof IllegalArgumentException) ? 400 : 500;
                String msg = (e instanceof IllegalArgumentException) ? e.getMessage() : "Erro interno do servidor";
                enviarErro(exchange, msg, status);
            }
            finally{
                exchange.close();
            }
        }
    }

    private void processarRota(HttpExchange exchange, String metodo, String path) throws IOException{
        if("POST".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
            MaterialControl.getInstancia().cadastrar(exchange);
        }
        else if("GET".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
            MaterialControl.getInstancia().listar(exchange);
        }
        else if("PUT".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
            MaterialControl.getInstancia().atualizar(exchange);
        }
        else if("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
            MaterialControl.getInstancia().excluir(exchange);
        }
        else{
            enviarErro(exchange, "Rota não encontrada", 404);
        }
    }

    private void enviarRespostaVazia(HttpExchange exchange, int status){
        try{
            exchange.sendResponseHeaders(status, -1);
        }
        catch (IOException e){
            System.err.println("Erro ao enviar cabeçalhos: " + e.getMessage());
        }
    }

    private void enviarErro(HttpExchange exchange, String mensagem, int status){
        try{
            String json = new Gson().toJson(java.util.Map.of("erro", mensagem));
            byte[] response = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try(OutputStream os = exchange.getResponseBody()){
                os.write(response);
            }
        }
        catch (IOException e){
            System.err.println("Falha crítica ao enviar erro ao cliente: " + e.getMessage());
        }
    }
}