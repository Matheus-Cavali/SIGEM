package org.example.dao;

import org.example.model.CategoriaDocumento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDocumentoDao {

    public int inserir(Connection conn, CategoriaDocumento categoria) throws SQLException {
        String sql = "INSERT INTO categoria_documento (nome) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, categoria.getNome());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public CategoriaDocumento buscarPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM categoria_documento WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public CategoriaDocumento buscarPorNome(Connection conn, String nome) throws SQLException {
        String sql = "SELECT * FROM categoria_documento WHERE LOWER(nome) = LOWER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public List<CategoriaDocumento> listar(Connection conn, String filtroNome) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_documento WHERE 1=1");
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");

        List<CategoriaDocumento> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(1, "%" + filtroNome + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        }
        return lista;
    }

    public boolean atualizar(Connection conn, CategoriaDocumento categoria) throws SQLException {
        String sql = "UPDATE categoria_documento SET nome = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria.getNome());
            stmt.setInt(2, categoria.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM categoria_documento WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    private CategoriaDocumento extrair(ResultSet rs) throws SQLException {
        CategoriaDocumento categoria = new CategoriaDocumento();
        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));
        return categoria;
    }
}
