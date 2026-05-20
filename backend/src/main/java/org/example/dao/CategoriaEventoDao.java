package org.example.dao;

import org.example.model.CategoriaEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaEventoDao {

    public int inserir(Connection conn, CategoriaEvento categoria) throws SQLException {

        String sql = "INSERT INTO categoria_evento (nome) VALUES (?)";

        int id = -1;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, categoria.getNome());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {

                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        }

        return id;
    }

    public CategoriaEvento buscarPorId(Connection conn, int id) throws SQLException {

        String sql = "SELECT * FROM categoria_evento WHERE id = ?";

        CategoriaEvento categoria = null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    categoria = extrair(rs);
                }
            }
        }

        return categoria;
    }

    public CategoriaEvento buscarPorNomeExato(Connection conn, String nome) throws SQLException {

        String sql = "SELECT * FROM categoria_evento WHERE UPPER(nome) = UPPER(?)";

        CategoriaEvento categoria = null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome.trim());

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    categoria = extrair(rs);
                }
            }
        }

        return categoria;
    }

    public List<CategoriaEvento> listar(Connection conn, String nome) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM categoria_evento WHERE 1=1"
        );

        List<String> params = new ArrayList<>();

        if (nome != null && !nome.trim().isEmpty()) {

            sql.append(" AND nome ILIKE ?");

            params.add("%" + nome.trim() + "%");
        }

        sql.append(" ORDER BY nome");

        List<CategoriaEvento> lista = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {

                stmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    lista.add(extrair(rs));
                }
            }
        }

        return lista;
    }

    public boolean atualizar(Connection conn, CategoriaEvento categoria) throws SQLException {

        String sql = "UPDATE categoria_evento SET nome = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoria.getNome());
            stmt.setInt(2, categoria.getId());

            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {

        String sql = "DELETE FROM categoria_evento WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
    }

    private CategoriaEvento extrair(ResultSet rs) throws SQLException {

        CategoriaEvento categoria = new CategoriaEvento();

        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));

        return categoria;
    }
}