package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.controller.CUsuario;
import org.example.controller.CInvestimento;
import org.example.controller.CColaborador;
import org.example.controller.CVoluntario;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.UsuarioDao;

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

            server.createContext("/api/login", CUsuario.getInstancia());
            server.createContext("/api/cadastrar", CUsuario.getInstancia());
            server.createContext("/api/alterar-Primeira-Senha", CUsuario.getInstancia());
            server.createContext("/api/cadastrar-interno", CUsuario.getInstancia());
            server.createContext("/api/usuarios", CUsuario.getInstancia());
            server.createContext("/api/investimentos", CInvestimento.getInstancia());
            server.createContext("/api/colaboradores", CColaborador.getInstancia());
            server.createContext("/api/voluntarios", CVoluntario.getInstancia());
            server.createContext("/api/recurso", CUsuario.getInstancia());

            server.setExecutor(null);
            server.start();

            System.out.println("Servidor SIGEM rodando na porta 8080...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}