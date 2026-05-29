package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.AporteInvestimento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AporteInvestimentoDao {

    public int inserir(Connection conn, AporteInvestimento aporte) {
        String sql = "INSERT INTO aporte_investimento (investimento_futuro_id, valor_aporte, data_aporte, colaborador_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            throw new DatabaseException("Erro ao inserir aporte de investimento", e);
        }
        return -1;
    }

    public boolean atualizar(Connection conn, int id, AporteInvestimento aporte) {
        String sql = "UPDATE aporte_investimento SET valor_aporte = ?, data_aporte = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, aporte.getValorAporte());
            stmt.setObject(2, aporte.getDataAporte());
            stmt.setInt(3, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar aporte de investimento", e);
        }
    }

    public boolean deletar(Connection conn, int id) {
        String sql = "DELETE FROM aporte_investimento WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new DatabaseException("Erro ao excluir aporte de investimento: há registros vinculados.", e);
            }
            throw new DatabaseException("Erro ao excluir aporte de investimento", e);
        }
    }

    public boolean deletarPorInvestimento(Connection conn, int investimentoId) {
        String sql = "DELETE FROM aporte_investimento WHERE investimento_futuro_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao deletar aportes por investimento", e);
        }
    }

    public List<AporteInvestimento> listarPorInvestimento(Connection conn, int investimentoId) {
        String sql = "SELECT a.*, u.nome AS colaborador_nome FROM aporte_investimento a " +
                "LEFT JOIN usuario u ON u.id = a.colaborador_id " +
                "WHERE a.investimento_futuro_id = ? ORDER BY a.data_aporte DESC";
        List<AporteInvestimento> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AporteInvestimento a = new AporteInvestimento();
                    a.setId(rs.getInt("id"));
                    a.setInvestimentoFuturoId(rs.getInt("investimento_futuro_id"));
                    a.setValorAporte(rs.getBigDecimal("valor_aporte"));
                    a.setDataAporte(rs.getObject("data_aporte", LocalDate.class));
                    a.setColaboradorId((Integer) rs.getObject("colaborador_id"));
                    a.setColaboradorNome(rs.getString("colaborador_nome"));
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar aportes por investimento", e);
        }
        return lista;
    }
}