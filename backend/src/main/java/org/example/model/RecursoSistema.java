package org.example.model;

import org.example.dao.RecursoSistemaDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.example.model.Usuario;

public class RecursoSistema {
    private int id;
    private String nome;
    private String descricao;

    private static RecursoSistemaDao dao;

    public static synchronized RecursoSistemaDao getDao() {
        if (dao == null) dao = new RecursoSistemaDao();
        return dao;
    }

    public RecursoSistema() {}

    public static List<RecursoSistema> listarTodos(Connection conn) throws SQLException {
        return getDao().listarTodos(conn);
    }

    public static List<RecursoSistema> listarPorUsuario(Connection conn, int usuarioId) throws SQLException {
        return getDao().listarPorUsuario(conn, usuarioId);
    }

    public static List<Usuario> listarUsuariosPorPermissao(Connection conn, String permissaoNome) throws SQLException {
        return getDao().listarUsuariosPorPermissao(conn, permissaoNome);
    }

    public static boolean atualizarPermissoes(Connection conn, int usuarioId, List<Integer> recursoIds) throws SQLException {
        return getDao().atualizarPermissoes(conn, usuarioId, recursoIds);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}