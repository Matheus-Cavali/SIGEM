package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.controller.ParametersControl;
import org.example.model.Resposta;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ParametersRouter implements HttpHandler {
    private static ParametersRouter instancia;
    private final ParametersControl controller = ParametersControl.getInstancia();

    private ParametersRouter() {}

    public static ParametersRouter getInstancia() {
        if (instancia == null) instancia = new ParametersRouter();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        aplicarCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        try {
            if ("GET".equalsIgnoreCase(metodo) && "/api/parameters".equals(path)) {
                enviarResposta(exchange, controller.buscar());
            } else if ("PUT".equalsIgnoreCase(metodo) && "/api/parameters".equals(path)) {
                String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                enviarResposta(exchange, controller.salvar(json));
            } else if ("POST".equalsIgnoreCase(metodo) && "/api/parameters/logo".equals(path)) {
                String caminhoLogo = salvarUpload(exchange);
                enviarResposta(exchange, controller.salvarLogo(caminhoLogo));
            } else {
                enviarJson(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        } catch (Exception e) {
            enviarJson(exchange, "{\"erro\":\"Erro interno\"}", 500);
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
            if (headers.contains("name=\"logo\"")) {
                String filename = extrairFilename(headers);
                String extension = extrairExtensao(filename);
                Path uploadDir = Path.of("uploads", "parameters").toAbsolutePath().normalize();
                Files.createDirectories(uploadDir);

                String fileName = "logo-" + System.currentTimeMillis() + extension;
                Path target = uploadDir.resolve(fileName);
                Files.write(target, java.util.Arrays.copyOfRange(body, dataStart, dataEnd));
                return "/uploads/parameters/" + fileName;
            }

            cursor = indexOf(body, boundaryBytes, nextBoundary + boundaryBytes.length);
        }

        throw new IOException("Arquivo logo não encontrado");
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
        if (start < 0) return "logo";
        int end = headers.indexOf("\"", start + marker.length());
        if (end < 0) return "logo";
        return headers.substring(start + marker.length(), end);
    }

    private String extrairExtensao(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return ".png";
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!extension.matches("\\.(png|jpg|jpeg|webp|gif)")) return ".png";
        return extension;
    }

    private void aplicarCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void enviarResposta(HttpExchange exchange, Resposta resposta) throws IOException {
        enviarJson(exchange, resposta.body, resposta.status);
    }

    private void enviarJson(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] respostaBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, respostaBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respostaBytes);
        }
    }
}
