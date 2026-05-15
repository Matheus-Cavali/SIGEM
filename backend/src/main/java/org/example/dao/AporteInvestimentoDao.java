package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.AporteInvestimento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AporteInvestimentoDao {

    public int inserir(AporteInvestimento aporte) {
        String sql = "INSERT INTO aporte_investimento (investimento_futuro_id, valor_aporte, data_aporte, colaborador_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, aporte.getInvestimentoFuturoId());
            stmt.setBigDecimal(2, aporte.getValorAporte());
            stmt.setObject(3, aporte.getDataAporte());
            if (aporte.getColaboradorId() != null) {
                stmt.setInt(4, aporte.getColaboradorId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir aporte: " + e.getMessage());
        }
        return -1;
    }

    public boolean atualizar(int id, AporteInvestimento aporte) {
        String sql = "UPDATE aporte_investimento SET valor_aporte = ?, data_aporte = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, aporte.getValorAporte());
            stmt.setObject(2, aporte.getDataAporte());
            stmt.setInt(3, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar aporte: " + e.getMessage());
            return false;
        }
    }

    public boolean deletar(int id) {
        String sql = "DELETE FROM aporte_investimento WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar aporte: " + e.getMessage());
            return false;
        }
    }

    public boolean deletarPorInvestimento(int investimentoId) {
        String sql = "DELETE FROM aporte_investimento WHERE investimento_futuro_id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar aportes do investimento: " + e.getMessage());
            return false;
        }
    }

    public List<AporteInvestimento> listarPorInvestimento(int investimentoId) {
        String sql = "SELECT a.*, u.nome AS colaborador_nome FROM aporte_investimento a LEFT JOIN usuario u ON u.id = a.colaborador_id WHERE a.investimento_futuro_id = ? ORDER BY a.data_aporte DESC";
        List<AporteInvestimento> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AporteInvestimento a = new AporteInvestimento();
                    a.setId(rs.getInt("id"));
                    a.setInvestimentoFuturoId(rs.getInt("investimento_futuro_id"));
                    a.setValorAporte(rs.getBigDecimal("valor_aporte"));
                    a.setDataAporte(rs.getObject("data_aporte", LocalDate.class));
                    a.setColaboradorId((Integer) rs.getObject("colaborador_id"));
                    try { a.setColaboradorNome(rs.getString("colaborador_nome")); } catch (SQLException e) {}
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar aportes: " + e.getMessage());
        }
        return lista;
    }
}