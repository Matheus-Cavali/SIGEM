package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.MaterialControl;
import org.example.model.Resposta;

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
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String metodo = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        if("OPTIONS".equalsIgnoreCase(metodo)){
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try{
            MaterialControl control = MaterialControl.getInstancia();
            Resposta r;

            if("POST".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = control.cadastrar(auth, json);
            }
            else if("GET".equalsIgnoreCase(metodo) && "/api/materiais".equals(path)){
                r = control.listar(exchange.getRequestURI().getQuery());
            }
            else if("PUT".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = control.atualizar(auth, id, json);
            }
            else if("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/materiais/\\d+")){
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                r = control.excluir(auth, id);
            }
            else{
                r = new Resposta(404, "{\"erro\":\"Rota não encontrada\"}");
            }

            enviarResposta(exchange, r.body, r.status);
        }
        catch (Exception e){
            System.err.println("ERRO: " + e.getMessage());
            enviarResposta(exchange, "{\"erro\":\"Erro interno do servidor\"}", 500);
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
