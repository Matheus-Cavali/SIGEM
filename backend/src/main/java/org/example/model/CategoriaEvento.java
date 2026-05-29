package org.example.model;

import org.example.dao.CategoriaEventoDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaEvento {

    private Integer id;
    private String nome;

    public CategoriaEvento() {}

    public CategoriaEvento(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaEvento(String nome) {
        this(null, nome);
    }

    public Map<String, String> validar() {

        Map<String, String> erros = new LinkedHashMap<>();

        if (nome == null || nome.trim().isEmpty()) {
            erros.put("nome", "Nome obrigatório");
        }

        return erros;
    }

    public static int salvar(Connection conn, CategoriaEvento categoria) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.inserir(conn, categoria);
    }

    public static CategoriaEvento buscarPorId(Connection conn, int id) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.buscarPorId(conn, id);
    }

    public static CategoriaEvento buscarPorNome(Connection conn, String nome) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.buscarPorNomeExato(conn, nome);
    }

    public static List<CategoriaEvento> listar(Connection conn, String nome) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.listar(conn, nome);
    }

    public static boolean atualizar(Connection conn, CategoriaEvento categoria) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.atualizar(conn, categoria);
    }

    public static boolean deletar(Connection conn, int id) throws SQLException {

        CategoriaEventoDao dao = new CategoriaEventoDao();

        return dao.deletar(conn, id);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}