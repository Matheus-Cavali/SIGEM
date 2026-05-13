package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.InvestimentoFuturo;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestimentoFuturoDao {

    public int inserir(InvestimentoFuturo inv) {
        String sql = "INSERT INTO investimento_futuro (nome, valor_meta, data_abertura, status, colaborador_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, "ABERTO");
            stmt.setInt(5, inv.getColaboradorId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir investimento: " + e.getMessage());
        }
        return -1;
    }

    public InvestimentoFuturo buscarPorId(int id) {
        String sql = "SELECT * FROM investimento_futuro WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    InvestimentoFuturo inv = extrair(rs);
                    inv.setSaldoAtual(calcularSaldo(id));
                    return inv;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar investimento: " + e.getMessage());
        }
        return null;
    }

    public InvestimentoFuturo buscarPorNome(String nome) {
        String sql = "SELECT id FROM investimento_futuro WHERE nome = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    InvestimentoFuturo inv = new InvestimentoFuturo();
                    inv.setId(rs.getInt("id"));
                    return inv;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar investimento por nome: " + e.getMessage());
        }
        return null;
    }

    public List<InvestimentoFuturo> listar(String filtroNome, String filtroStatus) {
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, COALESCE(SUM(a.valor_aporte), 0) AS saldo " +
            "FROM investimento_futuro i " +
            "LEFT JOIN aporte_investimento a ON a.investimento_futuro_id = i.id " +
            "WHERE 1=1"
        );
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND i.nome ILIKE ?");
        if (filtroStatus != null && !filtroStatus.isEmpty()) sql.append(" AND i.status = ?");
        sql.append(" GROUP BY i.id ORDER BY i.nome");

        List<InvestimentoFuturo> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(idx++, "%" + filtroNome + "%");
            if (filtroStatus != null && !filtroStatus.isEmpty()) stmt.setString(idx++, filtroStatus);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar investimentos: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizar(InvestimentoFuturo inv) {
        String sql = "UPDATE investimento_futuro SET nome = ?, valor_meta = ?, data_abertura = ?, status = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inv.getNome());
            stmt.setBigDecimal(2, inv.getValorMeta());
            stmt.setObject(3, inv.getDataAbertura());
            stmt.setString(4, inv.getStatus());
            stmt.setInt(5, inv.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar investimento: " + e.getMessage());
            return false;
        }
    }

    public void migrarStatus() {
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt1 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ABERTO' WHERE status IN ('PLANEJADO', 'EM_ANDAMENTO')");
             PreparedStatement stmt2 = conn.prepareStatement("UPDATE investimento_futuro SET status = 'ENCERRADO' WHERE status = 'CONCLUIDO'")) {
            int abertos = stmt1.executeUpdate();
            int encerrados = stmt2.executeUpdate();
            if (abertos > 0 || encerrados > 0) {
                System.out.println("Migração de status: " + abertos + " para ABERTO, " + encerrados + " para ENCERRADO");
            }
        } catch (SQLException e) {
            System.err.println("Erro na migração de status: " + e.getMessage());
        }
    }

    public boolean deletar(int id) {
        AporteInvestimentoDao apDao = new AporteInvestimentoDao();
        apDao.deletarPorInvestimento(id);

        String sql = "DELETE FROM investimento_futuro WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar investimento: " + e.getMessage());
            return false;
        }
    }

    public BigDecimal calcularSaldo(int investimentoId) {
        String sql = "SELECT COALESCE(SUM(valor_aporte), 0) FROM aporte_investimento WHERE investimento_futuro_id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investimentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao calcular saldo: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private InvestimentoFuturo extrair(ResultSet rs) throws SQLException {
        InvestimentoFuturo inv = new InvestimentoFuturo();
        inv.setId(rs.getInt("id"));
        inv.setNome(rs.getString("nome"));
        inv.setValorMeta(rs.getBigDecimal("valor_meta"));
        inv.setDataAbertura(rs.getObject("data_abertura", LocalDate.class));
        inv.setStatus(rs.getString("status"));
        inv.setColaboradorId(rs.getInt("colaborador_id"));
        try {
            inv.setSaldoAtual(rs.getBigDecimal("saldo"));
        } catch (SQLException e) {
            inv.setSaldoAtual(BigDecimal.ZERO);
        }
        return inv;
    }
}