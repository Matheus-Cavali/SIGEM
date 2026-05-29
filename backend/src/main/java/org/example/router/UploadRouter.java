package org.example.router;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class UploadRouter implements HttpHandler {
    private static UploadRouter instancia;

    private UploadRouter() {}

    public static UploadRouter getInstancia() {
        if (instancia == null) instancia = new UploadRouter();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
        } else {
            String relative = exchange.getRequestURI().getPath().replaceFirst("^/uploads/?", "");
            Path baseDir = Path.of("uploads").toAbsolutePath().normalize();
            Path file = baseDir.resolve(relative).normalize();

            if (!file.startsWith(baseDir) || !Files.exists(file) || Files.isDirectory(file)) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                String contentType = Files.probeContentType(file);
                exchange.getResponseHeaders().set("Content-Type", contentType != null ? contentType : "application/octet-stream");
                byte[] bytes = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }
}
