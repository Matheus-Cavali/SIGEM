package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.dao.UsuarioDao;
import org.example.router.*;

import java.net.InetSocketAddress;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            UsuarioDao dao = new UsuarioDao();
            dao.criarAdmin();

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/login", UsuarioRouter.getInstancia());
            server.createContext("/api/alterar-Primeira-Senha", UsuarioRouter.getInstancia());
            server.createContext("/api/cadastrar-interno", UsuarioRouter.getInstancia());
            server.createContext("/api/usuarios", UsuarioRouter.getInstancia());
            server.createContext("/api/investimentos", InvestimentoRouter.getInstancia());
            server.createContext("/api/colaboradores", ColaboradorRouter.getInstancia());
            server.createContext("/api/voluntarios", VoluntarioRouter.getInstancia());
            server.createContext("/api/categorias-materiais", new CategoriaMaterialRouter());
            server.createContext("/api/materiais", new MaterialRouter());
            server.createContext("/api/recurso", UsuarioRouter.getInstancia());
            server.createContext("/api/parameters", new ParametrizacaoIgrejaRouter());
            server.createContext("/uploads", UploadRouter.getInstancia());
            server.createContext("/api/categorias-eventos", CategoriaEventoRouter.getInstancia());
            server.createContext("/api/categorias-despesa", DespesaRouter.getInstancia());
            server.createContext("/api/despesas", DespesaRouter.getInstancia());
            server.createContext("/api/documentos", new DocumentoRouter());
            server.createContext("/api/categorias-documentos", new CategoriaDocumentoRouter());
            server.createContext("/api/doacoes-materiais", new DoacaoMaterialRouter());
            server.createContext("/api/caixas", new CaixaRouter());
            server.createContext("/api/eventos", new EventoRouter());
            server.createContext("/api/locais-evento", new LocalEventoRouter());

            server.setExecutor(null);
            server.start();

            System.out.println("Servidor SIGEM rodando na porta 8080...");

        } catch (IOException e) {
            throw new RuntimeException("Erro ao iniciar o servidor.", e);
        }
    }
}
