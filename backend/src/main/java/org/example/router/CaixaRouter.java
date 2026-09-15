package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.CaixaControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CaixaRouter implements HttpHandler {
    private static CaixaControl control;

    public static synchronized CaixaControl getControl(){
        if(control == null)
            control = CaixaControl.getInstancia();
        return control;
    }

    public CaixaRouter() {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String metodo = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        if("OPTIONS".equalsIgnoreCase(metodo)){
            exchange.sendResponseHeaders(204, -1);
        } else try {
            Resposta r;

            if("POST".equalsIgnoreCase(metodo) && "/api/caixas".equals(path)){
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = getControl().abrir(auth, json);
            }
            else if("GET".equalsIgnoreCase(metodo) && "/api/caixas/aberto".equals(path)){
                r = getControl().buscarAberto();
            }
            else if("GET".equalsIgnoreCase(metodo) && "/api/caixas".equals(path)){
                r = getControl().listar();
            }
            else if("PUT".equalsIgnoreCase(metodo) && path.matches("/api/caixas/\\d+/fechar")){
                int id = Integer.parseInt(path.split("/")[3]);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = getControl().fechar(auth, id, json);
            }
            else if("PATCH".equalsIgnoreCase(metodo) && path.matches("/api/caixas/\\d+/movimentar")){
                int id = Integer.parseInt(path.split("/")[3]);
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = getControl().movimentar(auth, id, json);
            }
            else{
                r = new Resposta(404, "{\"erro\":\"Rota não encontrada\"}");
            }

            enviarResposta(exchange, r.body, r.status);
        }
        catch(Exception e){
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
        catch(IOException e){
            System.err.println("Erro crítico de I/O: " + e.getMessage());
        }
    }
}
