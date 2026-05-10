package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.controller.CUsuario;
import org.example.dao.UsuarioDao;

import java.net.InetSocketAddress;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            UsuarioDao dao = new UsuarioDao();
            dao.criarAdmin();
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/login", CUsuario.getInstancia());
            server.createContext("/api/cadastrar", CUsuario.getInstancia());
            server.createContext("/api/alterar-Primeira-Senha", CUsuario.getInstancia());

            server.setExecutor(null);
            server.start();

            System.out.println("Servidor SIGEM rodando na porta 8080...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}