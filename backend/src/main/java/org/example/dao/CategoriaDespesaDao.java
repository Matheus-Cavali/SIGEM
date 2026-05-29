package org.example.dao;

import org.example.model.CategoriaDespesa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDespesaDao {

    public int inserir(Connection conn, CategoriaDespesa categoria) throws SQLException {
        String sql = "INSERT INTO categoria_despesa (nome) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, categoria.getNome());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public CategoriaDespesa buscarPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM categoria_despesa WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public CategoriaDespesa buscarPorNome(Connection conn, String nome) throws SQLException {
        String sql = "SELECT * FROM categoria_despesa WHERE LOWER(nome) = LOWER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public List<CategoriaDespesa> listar(Connection conn, String filtroNome) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_despesa WHERE 1=1");
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");

        List<CategoriaDespesa> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(1, "%" + filtroNome + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        }
        return lista;
    }

    public boolean atualizar(Connection conn, CategoriaDespesa categoria) throws SQLException {
        String sql = "UPDATE categoria_despesa SET nome = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria.getNome());
            stmt.setInt(2, categoria.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM categoria_despesa WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    private CategoriaDespesa extrair(ResultSet rs) throws SQLException {
        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));
        return categoria;
    }
}
