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

    private static DocumentoRouter instancia;
    private DocumentoRouter() {}
    public static DocumentoRouter getInstancia() {
        if (instancia == null) instancia = new DocumentoRouter();
        return instancia;
    }

    private DocumentoControl controller = DocumentoControl.getInstancia();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");

        try {
            if ("POST".equalsIgnoreCase(metodo) && "/api/categorias-documentos".equals(path)) {
                String json = lerBody(exchange);
                Resposta r = controller.criarCategoria(auth, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/categorias-documentos".equals(path)) {
                Resposta r = controller.listarCategorias(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/categorias-documentos/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.buscarCategoria(id);
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/categorias-documentos/\\d+")) {
                int id = extrairUltimoId(path);
                String json = lerBody(exchange);
                Resposta r = controller.atualizarCategoria(auth, id, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/categorias-documentos/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.deletarCategoria(auth, id);
                enviarResposta(exchange, r.body, r.status);

            } else if ("POST".equalsIgnoreCase(metodo) && "/api/documentos/upload".equals(path)) {
                Resposta autorizado = controller.autorizarUpload(auth);
                if (autorizado.status != 200) {
                    enviarResposta(exchange, autorizado.body, autorizado.status);
                } else {
                    String caminhoArquivo = salvarUpload(exchange);
                    enviarResposta(exchange, "{\"caminhoArquivo\":\"" + escaparJson(caminhoArquivo) + "\"}", 201);
                }

            } else if ("POST".equalsIgnoreCase(metodo) && "/api/documentos".equals(path)) {
                String json = lerBody(exchange);
                Resposta r = controller.cadastrar(auth, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && "/api/documentos".equals(path)) {
                Resposta r = controller.listar(exchange.getRequestURI().getQuery());
                enviarResposta(exchange, r.body, r.status);

            } else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.buscar(id);
                enviarResposta(exchange, r.body, r.status);

            } else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")) {
                int id = extrairUltimoId(path);
                String json = lerBody(exchange);
                Resposta r = controller.atualizar(auth, id, json);
                enviarResposta(exchange, r.body, r.status);

            } else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/documentos/\\d+")) {
                int id = extrairUltimoId(path);
                Resposta r = controller.deletar(auth, id);
                enviarResposta(exchange, r.body, r.status);

            } else {
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        } catch (Exception e) {
            System.err.println("ERRO DocumentoRouter: " + e.getMessage());
            enviarResposta(exchange, "{\"erro\":\"Erro interno\"}", 500);
        }
    }

    private String lerBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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

    private int extrairUltimoId(String path) {
        String[] partes = path.split("/");
        for (int i = partes.length - 1; i >= 0; i--) {
            if (partes[i].matches("\\d+")) return Integer.parseInt(partes[i]);
        }
        return -1;
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
