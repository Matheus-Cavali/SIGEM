package org.example.dao;

import org.example.model.ParametrizacaoIgreja;

import java.sql.*;

public class ParametrizacaoIgrejaDao {

    public ParametrizacaoIgreja buscar(Connection conn) throws SQLException {
        String sql = "SELECT * FROM parametrizacao_igreja ORDER BY id LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return extrair(rs);
        }
        return null;
    }

    public ParametrizacaoIgreja salvar(Connection conn, ParametrizacaoIgreja parametros) throws SQLException {
        ParametrizacaoIgreja atual = buscar(conn);
        if (atual == null) {
            return inserir(conn, parametros);
        }
        parametros.setId(atual.getId());
        return atualizar(conn, parametros);
    }

    private ParametrizacaoIgreja inserir(Connection conn, ParametrizacaoIgreja parametros) throws SQLException {
        String sql = """
            INSERT INTO parametrizacao_igreja
            (nome_fantasia, razao_social, cnpj, caminho_logo, cor_primaria, cor_secundaria, telefone, email, site, endereco_completo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherStatement(stmt, parametros);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) parametros.setId(rs.getInt(1));
            }
        }
        return parametros;
    }

    private ParametrizacaoIgreja atualizar(Connection conn, ParametrizacaoIgreja parametros) throws SQLException {
        String sql = """
            UPDATE parametrizacao_igreja
            SET nome_fantasia = ?, razao_social = ?, cnpj = ?, caminho_logo = ?, cor_primaria = ?,
                cor_secundaria = ?, telefone = ?, email = ?, site = ?, endereco_completo = ?
            WHERE id = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherStatement(stmt, parametros);
            stmt.setInt(11, parametros.getId());
            stmt.executeUpdate();
        }
        return parametros;
    }

    private void preencherStatement(PreparedStatement stmt, ParametrizacaoIgreja parametros) throws SQLException {
        stmt.setString(1, parametros.getNomeFantasia());
        stmt.setString(2, parametros.getRazaoSocial());
        stmt.setString(3, parametros.getCnpj());
        stmt.setString(4, parametros.getCaminhoLogo());
        stmt.setString(5, parametros.getCorPrimaria());
        stmt.setString(6, parametros.getCorSecundaria());
        stmt.setString(7, parametros.getTelefone());
        stmt.setString(8, parametros.getEmail());
        stmt.setString(9, parametros.getSite());
        stmt.setString(10, parametros.getEndereco());
    }

    private ParametrizacaoIgreja extrair(ResultSet rs) throws SQLException {
        ParametrizacaoIgreja parametros = new ParametrizacaoIgreja();
        parametros.setId(rs.getInt("id"));
        parametros.setNomeFantasia(rs.getString("nome_fantasia"));
        parametros.setRazaoSocial(rs.getString("razao_social"));
        parametros.setCnpj(rs.getString("cnpj"));
        parametros.setCaminhoLogo(rs.getString("caminho_logo"));
        parametros.setCorPrimaria(rs.getString("cor_primaria"));
        parametros.setCorSecundaria(rs.getString("cor_secundaria"));
        parametros.setTelefone(rs.getString("telefone"));
        parametros.setEmail(rs.getString("email"));
        parametros.setSite(rs.getString("site"));
        parametros.setEndereco(rs.getString("endereco_completo"));
        return parametros;
    }
}
