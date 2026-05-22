package org.example.dao;

import org.example.model.Voluntario;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VoluntarioDao {

    public void inserir(Connection conn, int usuarioId, String data) throws SQLException {
        String sql = "INSERT INTO voluntario (usuario_id, data_inicio) VALUES (?, ?)";
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

    public Voluntario buscarPorId(Connection conn, int usuarioId) throws SQLException {
        String sql = "SELECT u.*, v.data_inicio, v.data_desligamento FROM usuario u JOIN voluntario v ON u.id = v.usuario_id WHERE u.id = ?";
        Voluntario v = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) v = extrair(rs);
            }
        }
        return v;
    }

    public List<Voluntario> listar(Connection conn, String filtroNome, String filtroEmail) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT u.*, v.data_inicio, v.data_desligamento FROM usuario u JOIN voluntario v ON u.id = v.usuario_id WHERE 1=1"
        );
        List<String> params = new ArrayList<>();
        if (filtroNome != null && !filtroNome.isEmpty()) { sql.append(" AND u.nome ILIKE ?"); params.add("%" + filtroNome + "%"); }
        if (filtroEmail != null && !filtroEmail.isEmpty()) { sql.append(" AND u.email ILIKE ?"); params.add("%" + filtroEmail + "%"); }
        sql.append(" ORDER BY u.nome");

        List<Voluntario> lista = new ArrayList<>();
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

    public boolean atualizarDataDesligamento(Connection conn, int id, LocalDate data) throws SQLException {
        String sql = "UPDATE voluntario SET data_desligamento = ? WHERE usuario_id = ?";
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

    public boolean atualizar(Connection conn, Voluntario vol) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE voluntario SET data_inicio = ?, data_desligamento = ? WHERE usuario_id = ?")) {
            stmt.setObject(1, vol.getDataInicio());
            stmt.setObject(2, vol.getDataDesligamento());
            stmt.setInt(3, vol.getId());
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE usuario SET nome = ?, email = ?, celular = ? WHERE id = ?")) {
            stmt.setString(1, vol.getNome());
            stmt.setString(2, vol.getEmail());
            stmt.setString(3, vol.getCelular());
            stmt.setInt(4, vol.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    private Voluntario extrair(ResultSet rs) throws SQLException {
        Voluntario v = new Voluntario();
        v.setId(rs.getInt("id"));
        v.setNome(rs.getString("nome"));
        v.setEmail(rs.getString("email"));
        v.setCpf(rs.getString("cpf"));
        v.setCelular(rs.getString("celular"));
        v.setNivelAcesso(rs.getInt("nivel_acesso"));
        v.setStatusAtivo(rs.getBoolean("status_ativo"));
        v.setPrimeiroAcesso(rs.getBoolean("primeiro_acesso"));
        v.setTipoUsuario(rs.getString("tipo_usuario"));
        v.setDataInicio(rs.getObject("data_inicio", LocalDate.class));
        v.setDataDesligamento(rs.getObject("data_desligamento", LocalDate.class));
        return v;
    }
}
