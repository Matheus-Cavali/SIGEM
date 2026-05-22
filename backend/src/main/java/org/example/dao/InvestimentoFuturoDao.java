package org.example.dao;

import org.example.conexao.Conexao;
import org.example.exception.DatabaseException;
import org.example.model.InvestimentoFuturo;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestimentoFuturoDao {

    public int inserir(Connection conn, InvestimentoFuturo inv) {
        String sql = "INSERT INTO investimento_futuro (nome, valor_meta, data_abertura, status, colaborador_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, inv.getStatus() != null ? inv.getStatus() : "ABERTO");
            stmt.setInt(5, inv.getColaboradorId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao inserir investimento futuro", e);
        }
        return -1;
    }

    public InvestimentoFuturo buscarPorId(Connection conn, int id) {
        String sql = "SELECT i.*, COALESCE(SUM(a.valor_aporte), 0) AS saldo, u.nome AS colaborador_nome " +
                "FROM investimento_futuro i " +
                "LEFT JOIN aporte_investimento a ON a.investimento_futuro_id = i.id " +
                "LEFT JOIN usuario u ON u.id = i.colaborador_id " +
                "WHERE i.id = ? GROUP BY i.id, u.nome";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar investimento futuro por ID", e);
        }
        return null;
    }

    public InvestimentoFuturo buscarPorNome(Connection conn, String nome) {
        String sql = "SELECT id FROM investimento_futuro WHERE nome = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    InvestimentoFuturo inv = new InvestimentoFuturo();
                    inv.setId(rs.getInt("id"));
                    return inv;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar investimento futuro por nome", e);
        }
        return null;
    }

    public List<InvestimentoFuturo> listar(Connection conn, String filtroNome, String filtroStatus) {
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
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar investimentos futuros", e);
        }
        return lista;
    }

    public boolean atualizar(Connection conn, InvestimentoFuturo inv) {
        String sql = "UPDATE investimento_futuro SET nome = ?, valor_meta = ?, data_abertura = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, inv.getStatus());
            stmt.setInt(5, inv.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar investimento futuro", e);
        }
    }

    public boolean deletar(Connection conn, int id) {
        String sql = "DELETE FROM investimento_futuro WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new DatabaseException("Erro ao excluir investimento futuro: há registros vinculados.", e);
            }
            throw new DatabaseException("Erro ao excluir investimento futuro", e);
        }
    }

    public BigDecimal calcularSaldo(Connection conn, int investimentoId) {
        String sql = "SELECT COALESCE(SUM(valor_aporte), 0) FROM aporte_investimento WHERE investimento_futuro_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao calcular saldo", e);
        }
        return BigDecimal.ZERO;
    }

    public void migrarStatus() {
        try (Connection conn = Conexao.getConexao()) {
            migrarStatus(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Erro na conexao para migrar status", e);
        }
    }

    public void migrarStatus(Connection conn) {
        try (PreparedStatement stmt1 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ABERTO' WHERE status IN ('PLANEJADO', 'EM_ANDAMENTO')");
             PreparedStatement stmt2 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ENCERRADO' WHERE status = 'CONCLUIDO'")) {
            int abertos = stmt1.executeUpdate();
            int encerrados = stmt2.executeUpdate();
            if (abertos > 0 || encerrados > 0) {
                System.out.println("Migração de status: " + abertos + " para ABERTO, " + encerrados + " para ENCERRADO");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro na migração de status", e);
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