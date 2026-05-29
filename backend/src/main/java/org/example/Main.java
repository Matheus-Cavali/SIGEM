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

            server.createContext("/api/login", new UsuarioRouter());
            server.createContext("/api/alterar-Primeira-Senha", new UsuarioRouter());
            server.createContext("/api/cadastrar-interno", new UsuarioRouter());
            server.createContext("/api/usuarios", new UsuarioRouter());
            server.createContext("/api/investimentos", new InvestimentoRouter());
            server.createContext("/api/colaboradores", new ColaboradorRouter());
            server.createContext("/api/voluntarios", new VoluntarioRouter());
            server.createContext("/api/categorias-materiais", new CategoriaMaterialRouter());
            server.createContext("/api/materiais", new MaterialRouter());
            server.createContext("/api/recurso", new UsuarioRouter());
            server.createContext("/api/parameters", new ParametrizacaoIgrejaRouter());
            server.createContext("/uploads", UploadRouter.getInstancia());
            server.createContext("/api/categorias-eventos", new CategoriaEventoRouter());
            server.createContext("/api/categorias-despesa", DespesaRouter.getInstancia());
            server.createContext("/api/despesas", DespesaRouter.getInstancia());
            server.createContext("/api/documentos", new DocumentoRouter());
            server.createContext("/api/categorias-documentos", new CategoriaDocumentoRouter());
            server.createContext("/api/doacoes-materiais", new DoacaoMaterialRouter());
            server.createContext("/api/doacoes-financeiras", new DoacaoFinanceiraRouter());
            server.createContext("/api/categorias-financeiras", new CategoriaFinanceiraRouter());
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
