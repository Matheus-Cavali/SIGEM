package org.example.dao;

import org.example.model.AporteInvestimento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AporteInvestimentoDao {

    public int inserir(Connection conn, AporteInvestimento aporte) throws SQLException {
        String sql = "INSERT INTO aporte_investimento (investimento_futuro_id, valor_aporte, data_aporte, colaborador_id) VALUES (?, ?, ?, ?)";
        int id = -1;
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
                if (rs.next()) id = rs.getInt(1);
            }
        }
        return id;
    }

    public boolean atualizar(Connection conn, int id, AporteInvestimento aporte) throws SQLException {
        String sql = "UPDATE aporte_investimento SET valor_aporte = ?, data_aporte = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, aporte.getValorAporte());
            stmt.setObject(2, aporte.getDataAporte());
            stmt.setInt(3, id);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM aporte_investimento WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletarPorInvestimento(Connection conn, int investimentoId) throws SQLException {
        String sql = "DELETE FROM aporte_investimento WHERE investimento_futuro_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            stmt.executeUpdate();
            return true;
        }
    }

    public List<AporteInvestimento> listarPorInvestimento(Connection conn, int investimentoId) throws SQLException {
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
        }
        return lista;
    }
}
