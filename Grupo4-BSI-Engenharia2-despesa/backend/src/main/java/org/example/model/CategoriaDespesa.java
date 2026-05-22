package org.example.model;

import org.example.dao.CategoriaDespesaDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CategoriaDespesa {
    private int id;
    private String nome;

    public CategoriaDespesa() {}

    public static int inserir(Connection conn, CategoriaDespesa cd) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.inserir(conn, cd);
    }

    public static boolean atualizar(Connection conn, CategoriaDespesa cd) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.atualizar(conn, cd);
    }

    public static boolean deletar(Connection conn, int id) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.deletar(conn, id);
    }

    public static CategoriaDespesa buscarPorId(Connection conn, int id) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.buscarPorId(conn, id);
    }

    public static CategoriaDespesa buscarPorNome(Connection conn, String nome) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.buscarPorNome(conn, nome);
    }

    public static List<CategoriaDespesa> listar(Connection conn, String nome) throws SQLException {
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        return dao.listar(conn, nome);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
