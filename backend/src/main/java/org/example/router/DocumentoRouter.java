package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.DocumentoControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class DocumentoRouter implements HttpHandler {
    private static DocumentoControl control;

    public static synchronized DocumentoControl getControl() {
        if (control == null)
            control = new DocumentoControl();
        return control;
    }

    public DocumentoRouter() {}

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
            Resposta r;

            if("POST".equalsIgnoreCase(metodo) && "/api/documentos/upload".equals(path)){
                Resposta autorizado = getControl().autorizarUpload(auth);
                if (autorizado.status != 200) {
                    r = autorizado;
                }
                else {
                    String caminhoArquivo = salvarUpload(exchange);
                    r = new Resposta(201, "{\"caminhoArquivo\":\"" + escaparJson(caminhoArquivo) + "\"}");
                }
            }
            else if("POST".equalsIgnoreCase(metodo) && "/api/documentos".equals(path)){
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = getControl().cadastrar(auth, json);
            }
            else if("GET".equalsIgnoreCase(metodo) && "/api/documentos".equals(path)){
                r = getControl().listar(exchange.getRequestURI().getQuery());
            }
            else if("GET".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")){
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                r = getControl().buscarPorId(id);
            }
            else if("PUT".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")){
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                r = getControl().atualizar(auth, id, json);
            }
            else if("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")){
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                r = getControl().excluir(auth, id);
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

    private String salvarUpload(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null || !contentType.contains("multipart/form-data")) {
            throw new IOException("Requisição precisa ser multipart/form-data");
        }

        String boundary = "--" + contentType.substring(contentType.indexOf("boundary=") + 9);
        byte[] body = exchange.getRequestBody().readAllBytes();
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.ISO_8859_1);
        int cursor = indexOf(body, boundaryBytes, 0);

        while (cursor >= 0) {
            int headersStart = cursor + boundaryBytes.length + 2;
            int headersEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1), headersStart);
            if (headersEnd < 0) break;

            String headers = new String(body, headersStart, headersEnd - headersStart, StandardCharsets.ISO_8859_1);
            int dataStart = headersEnd + 4;
            int nextBoundary = indexOf(body, boundaryBytes, dataStart);
            if (nextBoundary < 0) break;

            int dataEnd = nextBoundary - 2;
            if (headers.contains("name=\"arquivo\"")) {
                String filename = extrairFilename(headers);
                String extension = extrairExtensao(filename);
                Path uploadDir = Path.of("uploads", "documentos").toAbsolutePath().normalize();
                Files.createDirectories(uploadDir);

                String fileName = "documento-" + System.currentTimeMillis() + extension;
                Path target = uploadDir.resolve(fileName);
                Files.write(target, java.util.Arrays.copyOfRange(body, dataStart, dataEnd));
                return "/uploads/documentos/" + fileName;
            }

            cursor = indexOf(body, boundaryBytes, nextBoundary + boundaryBytes.length);
        }

        throw new IOException("Arquivo não encontrado");
    }

    private int indexOf(byte[] source, byte[] target, int from) {
        for (int i = Math.max(0, from); i <= source.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    private String extrairFilename(String headers) {
        String marker = "filename=\"";
        int start = headers.indexOf(marker);
        if (start < 0) return "documento";
        int end = headers.indexOf("\"", start + marker.length());
        if (end < 0) return "documento";
        return headers.substring(start + marker.length(), end);
    }

    private String extrairExtensao(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return ".bin";
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!extension.matches("\\.(pdf|doc|docx|xls|xlsx|png|jpg|jpeg|webp|txt)")) return ".bin";
        return extension;
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
