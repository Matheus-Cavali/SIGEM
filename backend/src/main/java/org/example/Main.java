package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.UsuarioDao;
import org.example.router.*;

import java.net.InetSocketAddress;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            UsuarioDao dao = new UsuarioDao();
            dao.criarAdmin();
            InvestimentoFuturoDao invDao = new InvestimentoFuturoDao();
            invDao.migrarStatus();
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/login", UsuarioRouter.getInstancia());
            server.createContext("/api/cadastrar", UsuarioRouter.getInstancia());
            server.createContext("/api/alterar-Primeira-Senha", UsuarioRouter.getInstancia());
            server.createContext("/api/cadastrar-interno", UsuarioRouter.getInstancia());
            server.createContext("/api/usuarios", UsuarioRouter.getInstancia());
            server.createContext("/api/investimentos", InvestimentoRouter.getInstancia());
            server.createContext("/api/colaboradores", ColaboradorRouter.getInstancia());
            server.createContext("/api/voluntarios", VoluntarioRouter.getInstancia());
            server.createContext("/api/materiais", MaterialRouter.getInstancia());
            server.createContext("/api/recurso", UsuarioRouter.getInstancia());

            server.setExecutor(null);
            server.start();

            System.out.println("Servidor SIGEM rodando na porta 8080...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}
