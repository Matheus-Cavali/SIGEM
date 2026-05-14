package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.UsuarioDao;
import org.example.view.VColaborador;
import org.example.view.VInvestimento;
import org.example.view.VUsuario;
import org.example.view.VVoluntario;

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

            server.createContext("/api/login", VUsuario.getInstancia());
            server.createContext("/api/cadastrar", VUsuario.getInstancia());
            server.createContext("/api/alterar-Primeira-Senha", VUsuario.getInstancia());
            server.createContext("/api/cadastrar-interno", VUsuario.getInstancia());
            server.createContext("/api/usuarios", VUsuario.getInstancia());
            server.createContext("/api/investimentos", VInvestimento.getInstancia());
            server.createContext("/api/colaboradores", VColaborador.getInstancia());
            server.createContext("/api/voluntarios", VVoluntario.getInstancia());
            server.createContext("/api/recurso", VUsuario.getInstancia());

            server.setExecutor(null);
            server.start();

            System.out.println("Servidor SIGEM rodando na porta 8080...");

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}
