package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.InvestimentoFuturo;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestimentoFuturoDao {

    public int inserir(Connection conn, InvestimentoFuturo inv) throws SQLException {
        String sql = "INSERT INTO investimento_futuro (nome, valor_meta, data_abertura, status, colaborador_id) VALUES (?, ?, ?, ?, ?)";
        int id = -1;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, inv.getStatus() != null ? inv.getStatus() : "ABERTO");
            stmt.setInt(5, inv.getColaboradorId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) id = rs.getInt(1);
            }
        }
        return id;
    }

    public InvestimentoFuturo buscarPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT i.*, COALESCE(SUM(a.valor_aporte), 0) AS saldo, u.nome AS colaborador_nome " +
                     "FROM investimento_futuro i " +
                     "LEFT JOIN aporte_investimento a ON a.investimento_futuro_id = i.id " +
                     "LEFT JOIN usuario u ON u.id = i.colaborador_id " +
                     "WHERE i.id = ? GROUP BY i.id, u.nome";
        InvestimentoFuturo inv = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) inv = extrair(rs);
            }
        }
        return inv;
    }

    public InvestimentoFuturo buscarPorNome(Connection conn, String nome) throws SQLException {
        String sql = "SELECT id FROM investimento_futuro WHERE nome = ?";
        InvestimentoFuturo inv = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    inv = new InvestimentoFuturo();
                    inv.setId(rs.getInt("id"));
                }
            }
        }
        return inv;
    }

    public List<InvestimentoFuturo> listar(Connection conn, String filtroNome, String filtroStatus) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, COALESCE(SUM(a.valor_aporte), 0) AS saldo, u.nome AS colaborador_nome " +
            "FROM investimento_futuro i " +
            "LEFT JOIN aporte_investimento a ON a.investimento_futuro_id = i.id " +
            "LEFT JOIN usuario u ON u.id = i.colaborador_id " +
            "WHERE 1=1"
        );
        List<String> params = new ArrayList<>();
        if (filtroNome != null && !filtroNome.isEmpty()) { sql.append(" AND i.nome ILIKE ?"); params.add("%" + filtroNome + "%"); }
        if (filtroStatus != null && !filtroStatus.isEmpty()) { sql.append(" AND i.status = ?"); params.add(filtroStatus); }
        sql.append(" GROUP BY i.id, u.nome ORDER BY i.nome");

        List<InvestimentoFuturo> lista = new ArrayList<>();
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

    public boolean atualizar(Connection conn, InvestimentoFuturo inv) throws SQLException {
        String sql = "UPDATE investimento_futuro SET nome = ?, valor_meta = ?, data_abertura = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, inv.getStatus());
            stmt.setInt(5, inv.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM investimento_futuro WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    public BigDecimal calcularSaldo(Connection conn, int investimentoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor_aporte), 0) FROM aporte_investimento WHERE investimento_futuro_id = ?";
        BigDecimal saldo = BigDecimal.ZERO;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) saldo = rs.getBigDecimal(1);
            }
        }
        return saldo;
    }

    public void migrarStatus() {
        try (Connection conn = Conexao.getConexao()) {
            migrarStatus(conn);
        } catch (SQLException e) {
            System.err.println("Erro na migracao de status: " + e.getMessage());
        }
    }

    public void migrarStatus(Connection conn) throws SQLException {
        try (PreparedStatement stmt1 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ABERTO' WHERE status IN ('PLANEJADO', 'EM_ANDAMENTO')");
             PreparedStatement stmt2 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ENCERRADO' WHERE status = 'CONCLUIDO'")) {
            int abertos = stmt1.executeUpdate();
            int encerrados = stmt2.executeUpdate();
            if (abertos > 0 || encerrados > 0) {
                System.out.println("Migracao de status: " + abertos + " para ABERTO, " + encerrados + " para ENCERRADO");
            }
        }
    }

    private InvestimentoFuturo extrair(ResultSet rs) throws SQLException {
        InvestimentoFuturo inv = new InvestimentoFuturo();
        inv.setId(rs.getInt("id"));
        inv.setNome(rs.getString("nome"));
        inv.setValorMeta(rs.getBigDecimal("valor_meta"));
        inv.setDataAbertura(rs.getObject("data_abertura", LocalDate.class));
        inv.setStatus(rs.getString("status"));
        inv.setColaboradorId(rs.getInt("colaborador_id"));
        inv.setColaboradorNome(rs.getString("colaborador_nome"));
        try {
            inv.setSaldoAtual(rs.getBigDecimal("saldo"));
        } catch (SQLException e) {
            inv.setSaldoAtual(BigDecimal.ZERO);
        }
        return inv;
    }
}
