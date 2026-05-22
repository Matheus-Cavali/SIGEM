package org.example.dao;

import org.example.model.Colaborador;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ColaboradorDao {

    public void inserir(Connection conn, int usuarioId, String data) throws SQLException {
        String sql = "INSERT INTO colaborador (usuario_id, data_admissao) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDate dataConvertida;
            if (data != null && !data.trim().isEmpty()) {
                DateTimeFormatter formato = DateTimeFormatter.ofPattern("ddMMyyyy");
                dataConvertida = LocalDate.parse(data, formato);
            } else {
                dataConvertida = LocalDate.now();
            }
            stmt.setInt(1, usuarioId);
            stmt.setObject(2, dataConvertida);
            stmt.executeUpdate();
        }
    }

    public Colaborador buscarPorId(Connection conn, int usuarioId) throws SQLException {
        String sql = "SELECT u.*, c.data_admissao, c.data_demissao FROM usuario u JOIN colaborador c ON u.id = c.usuario_id WHERE u.id = ?";
        Colaborador c = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) c = extrair(rs);
            }
        }
        return c;
    }

    public List<Colaborador> listar(Connection conn, String filtroNome, String filtroEmail) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT u.*, c.data_admissao, c.data_demissao FROM usuario u JOIN colaborador c ON u.id = c.usuario_id WHERE 1=1"
        );
        List<String> params = new ArrayList<>();
        if (filtroNome != null && !filtroNome.isEmpty()) { sql.append(" AND u.nome ILIKE ?"); params.add("%" + filtroNome + "%"); }
        if (filtroEmail != null && !filtroEmail.isEmpty()) { sql.append(" AND u.email ILIKE ?"); params.add("%" + filtroEmail + "%"); }
        sql.append(" ORDER BY u.nome");

        List<Colaborador> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        }
        return lista;
    }

    public boolean atualizarDataDemissao(Connection conn, int id, LocalDate data) throws SQLException {
        String sql = "UPDATE colaborador SET data_demissao = ? WHERE usuario_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (data != null) {
                stmt.setObject(1, data);
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setInt(2, id);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean atualizar(Connection conn, Colaborador colab) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE colaborador SET data_admissao = ?, data_demissao = ? WHERE usuario_id = ?")) {
            stmt.setObject(1, colab.getDataAdmissao());
            stmt.setObject(2, colab.getDataDemissao());
            stmt.setInt(3, colab.getId());
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE usuario SET nome = ?, email = ?, celular = ? WHERE id = ?")) {
            stmt.setString(1, colab.getNome());
            stmt.setString(2, colab.getEmail());
            stmt.setString(3, colab.getCelular());
            stmt.setInt(4, colab.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    private Colaborador extrair(ResultSet rs) throws SQLException {
        Colaborador c = new Colaborador();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setEmail(rs.getString("email"));
        c.setCpf(rs.getString("cpf"));
        c.setCelular(rs.getString("celular"));
        c.setNivelAcesso(rs.getInt("nivel_acesso"));
        c.setStatusAtivo(rs.getBoolean("status_ativo"));
        c.setPrimeiroAcesso(rs.getBoolean("primeiro_acesso"));
        c.setTipoUsuario(rs.getString("tipo_usuario"));
        c.setDataAdmissao(rs.getObject("data_admissao", LocalDate.class));
        c.setDataDemissao(rs.getObject("data_demissao", LocalDate.class));
        return c;
    }
}
