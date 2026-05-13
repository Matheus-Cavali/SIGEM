package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Voluntario;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VoluntarioDao {

    public void inserirVoluntario(int id, String data) {
        String sql = "INSERT INTO voluntario (usuario_id, data_inicio) VALUES (?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy");
            LocalDate dataConvertida = LocalDate.parse(data, formato);
            stmt.setInt(1, id);
            stmt.setObject(2, dataConvertida);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao vincular voluntário: " + e.getMessage());
        }
    }

    public Voluntario buscarPorId(int usuarioId) {
        String sql = "SELECT u.*, v.data_inicio, v.data_desligamento FROM usuario u JOIN voluntario v ON u.id = v.usuario_id WHERE u.id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar voluntário: " + e.getMessage());
        }
        return null;
    }

    public List<Voluntario> listar(String filtroNome, String filtroEmail) {
        StringBuilder sql = new StringBuilder(
            "SELECT u.*, v.data_inicio, v.data_desligamento FROM usuario u JOIN voluntario v ON u.id = v.usuario_id WHERE 1=1"
        );
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND u.nome ILIKE ?");
        if (filtroEmail != null && !filtroEmail.isEmpty()) sql.append(" AND u.email ILIKE ?");
        sql.append(" ORDER BY u.nome");

        List<Voluntario> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(i++, "%" + filtroNome + "%");
            if (filtroEmail != null && !filtroEmail.isEmpty()) stmt.setString(i++, "%" + filtroEmail + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar voluntários: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizarDataDesligamento(int id, LocalDate data) {
        String sql = "UPDATE voluntario SET data_desligamento = ? WHERE usuario_id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (data != null) {
                stmt.setObject(1, data);
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setInt(2, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar data desligamento: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Voluntario vol) {
        String sql = "UPDATE voluntario SET data_inicio = ?, data_desligamento = ? WHERE usuario_id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, vol.getDataInicio());
            stmt.setObject(2, vol.getDataDesligamento());
            stmt.setInt(3, vol.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar voluntário: " + e.getMessage());
            return false;
        }

        String sqlUsuario = "UPDATE usuario SET nome = ?, email = ?, celular = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlUsuario)) {
            stmt.setString(1, vol.getNome());
            stmt.setString(2, vol.getEmail());
            stmt.setString(3, vol.getCelular());
            stmt.setInt(4, vol.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar dados do voluntário: " + e.getMessage());
            return false;
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