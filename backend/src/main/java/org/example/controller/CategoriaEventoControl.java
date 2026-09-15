package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;

import org.example.model.CategoriaEvento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaEventoControl {

    private static CategoriaEventoControl instancia;
    private final Gson gson = new Gson();

    private CategoriaEventoControl() {}

    public static synchronized CategoriaEventoControl getInstancia() {
        if (instancia == null) instancia = new CategoriaEventoControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        String email = emailDoToken(auth);
        if (email != null) {
            try {
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1)
                        return true;
                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome))
                            return true;
                    }
                }
            }
            catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public Resposta cadastrar(String auth, String json) {
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            CategoriaEvento categoria = gson.fromJson(json, CategoriaEvento.class);
            Map<String, String> erros = categoria.validar();
            if (!erros.isEmpty()) {
                return new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
            }
            if (CategoriaEvento.buscarPorNome(conn, categoria.getNome()) != null) {
                Map<String, String> err = new LinkedHashMap<>();
                err.put("nome", "Categoria de evento já cadastrada");
                return new Resposta(409, gson.toJson(Collections.singletonMap("erros", err)));
            }
            int id = CategoriaEvento.salvar(conn, categoria);
            return new Resposta(201, "{\"mensagem\":\"Categoria cadastrada com sucesso\",\"id\":" + id + "}");
        }
        catch (Exception e) {
            System.err.println("Erro ao cadastrar categoria: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Falha ao cadastrar categoria.\"}");
        }
    }

    public Resposta listar(String auth, String query) {
        if (emailDoToken(auth) == null) {
            return new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        }
        try {
            String nome = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "nome".equalsIgnoreCase(pair[0])) {
                        nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }

            List<CategoriaEvento> lista = CategoriaEvento.listar(Conexao.getConexao(), nome);
            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e) {
            System.err.println("Erro ao listar categorias: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias.\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json) {
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            CategoriaEvento categoria = gson.fromJson(json, CategoriaEvento.class);
            categoria.setId(id);
            Map<String, String> erros = categoria.validar();
            if (!erros.isEmpty()) {
                return new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
            }
            if (CategoriaEvento.buscarPorId(conn, id) == null) {
                return new Resposta(404, "{\"erro\":\"Categoria não encontrada\"}");
            }
            CategoriaEvento duplicada = CategoriaEvento.buscarPorNome(conn, categoria.getNome());
            if (duplicada != null && !duplicada.getId().equals(id)) {
                Map<String, String> err = new LinkedHashMap<>();
                err.put("nome", "Categoria já cadastrada");
                return new Resposta(409, gson.toJson(Collections.singletonMap("erros", err)));
            }
            CategoriaEvento.atualizar(conn, categoria);
            return new Resposta(200, "{\"mensagem\":\"Categoria atualizada com sucesso\"}");
        }
        catch (Exception e) {
            System.err.println("Erro ao atualizar categoria: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar categoria.\"}");
        }
    }

    public Resposta excluir(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try {
            Connection conn = Conexao.getConexao();
            if (CategoriaEvento.buscarPorId(conn, id) == null) {
                return new Resposta(404, "{\"erro\":\"Categoria não encontrada\"}");
            }
            CategoriaEvento.deletar(conn, id);
            return new Resposta(200, "{\"mensagem\":\"Categoria removida com sucesso\"}");
        }
        catch (Exception e) {
            System.err.println("Erro ao excluir categoria: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Falha ao excluir categoria.\"}");
        }
    }
}