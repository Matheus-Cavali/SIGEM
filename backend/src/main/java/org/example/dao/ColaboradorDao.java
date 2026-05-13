package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Colaborador;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ColaboradorDao {

    public void inserirColaborador(int id, String data) {
        String sql = "INSERT INTO colaborador (usuario_id, data_admissao) VALUES (?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy");
            LocalDate dataConvertida = LocalDate.parse(data, formato);
            stmt.setInt(1, id);
            stmt.setObject(2, dataConvertida);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao vincular colaborador: " + e.getMessage());
        }
    }

    public Colaborador buscarPorId(int usuarioId) {
        String sql = "SELECT u.*, c.data_admissao, c.data_demissao FROM usuario u JOIN colaborador c ON u.id = c.usuario_id WHERE u.id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar colaborador: " + e.getMessage());
        }
        return null;
    }

    public List<Colaborador> listar(String filtroNome, String filtroEmail) {
        StringBuilder sql = new StringBuilder(
            "SELECT u.*, c.data_admissao, c.data_demissao FROM usuario u JOIN colaborador c ON u.id = c.usuario_id WHERE 1=1"
        );
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND u.nome ILIKE ?");
        if (filtroEmail != null && !filtroEmail.isEmpty()) sql.append(" AND u.email ILIKE ?");
        sql.append(" ORDER BY u.nome");

        List<Colaborador> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(i++, "%" + filtroNome + "%");
            if (filtroEmail != null && !filtroEmail.isEmpty()) stmt.setString(i++, "%" + filtroEmail + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar colaboradores: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizarDataDemissao(int id, LocalDate data) {
        String sql = "UPDATE colaborador SET data_demissao = ? WHERE usuario_id = ?";
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
            System.err.println("Erro ao atualizar data demissão: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Colaborador colab) {
        String sql = "UPDATE colaborador SET data_admissao = ?, data_demissao = ? WHERE usuario_id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, colab.getDataAdmissao());
            stmt.setObject(2, colab.getDataDemissao());
            stmt.setInt(3, colab.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar colaborador: " + e.getMessage());
            return false;
        }

        String sqlUsuario = "UPDATE usuario SET nome = ?, email = ?, celular = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlUsuario)) {
            stmt.setString(1, colab.getNome());
            stmt.setString(2, colab.getEmail());
            stmt.setString(3, colab.getCelular());
            stmt.setInt(4, colab.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar dados do colaborador: " + e.getMessage());
            return false;
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