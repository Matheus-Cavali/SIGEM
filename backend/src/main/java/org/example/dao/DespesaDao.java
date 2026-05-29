package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Despesa;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DespesaDao {

    public int inserir(Connection conn, Despesa despesa) throws SQLException {
        String sql = "INSERT INTO despesa (descricao, valor, data_lancamento, data_vencimento, data_prazo, categoria_despesa_id, colaborador_lancou_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, despesa.getDescricao());
            stmt.setBigDecimal(2, despesa.getValor());
            stmt.setObject(3, despesa.getDataLancamento() != null ? despesa.getDataLancamento() : LocalDate.now());
            stmt.setObject(4, despesa.getDataVencimento());
            stmt.setObject(5, despesa.getDataPrazo());
            stmt.setInt(6, despesa.getCategoriaDespesaId());
            if (despesa.getColaboradorLancouId() != null) {
                stmt.setInt(7, despesa.getColaboradorLancouId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao cadastrar despesa", e);
        }
        return -1;
    }

    public Despesa buscarPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT d.*, t.nome AS tipo_nome FROM despesa d " +
                "LEFT JOIN categoria_despesa t ON t.id = d.categoria_despesa_id WHERE d.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar despesa", e);
        }
        return null;
    }

    public List<Despesa> listar(Connection conn, String filtroDescricao, Integer filtroTipoId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT d.*, t.nome AS tipo_nome FROM despesa d " +
                        "LEFT JOIN categoria_despesa t ON t.id = d.categoria_despesa_id WHERE 1=1"
        );
        if (filtroDescricao != null && !filtroDescricao.trim().isEmpty())
            sql.append(" AND d.descricao ILIKE ?");
        if (filtroTipoId != null)
            sql.append(" AND d.categoria_despesa_id = ?");
        sql.append(" ORDER BY d.data_vencimento, d.id");

        List<Despesa> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (filtroDescricao != null && !filtroDescricao.trim().isEmpty())
                stmt.setString(idx++, "%" + filtroDescricao.trim() + "%");
            if (filtroTipoId != null)
                stmt.setInt(idx++, filtroTipoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar despesas", e);
        }
        return lista;
    }

    public boolean atualizar(Connection conn, Despesa despesa) throws SQLException {
        String sql = "UPDATE despesa SET descricao = ?, valor = ?, data_vencimento = ?, data_prazo = ?, categoria_despesa_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, despesa.getDescricao());
            stmt.setBigDecimal(2, despesa.getValor());
            stmt.setObject(3, despesa.getDataVencimento());
            stmt.setObject(4, despesa.getDataPrazo());
            stmt.setInt(5, despesa.getCategoriaDespesaId());
            stmt.setInt(6, despesa.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar despesa", e);
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM despesa WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir despesa: há registros vinculados.", e);
            throw new DatabaseException("Erro ao excluir despesa", e);
        }
    }

    public boolean quitar(Connection conn, int id, LocalDate dataPagamento, java.math.BigDecimal valorPago) throws SQLException {
        String sql = "UPDATE despesa SET data_pagamento = ?, valor_pago = ? WHERE id = ? AND data_pagamento IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, dataPagamento);
            stmt.setBigDecimal(2, valorPago);
            stmt.setInt(3, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao quitar despesa", e);
        }
    }

    public BigDecimal calcularSaldo(Connection conn) throws SQLException {
        // Cria tabela de ajuste de saldo para testes se não existir
        try (java.sql.Statement s = conn.createStatement()) {
            s.executeUpdate("ALTER TABLE despesa ADD COLUMN IF NOT EXISTS data_prazo DATE");
        } catch (SQLException ignored2) {}
        try (java.sql.Statement s3 = conn.createStatement()) {
            s3.executeUpdate("ALTER TABLE despesa ADD COLUMN IF NOT EXISTS valor_pago NUMERIC(15,2)");
        } catch (SQLException ignored2) {}
        try (java.sql.Statement s2 = conn.createStatement()) {
            s2.executeUpdate("CREATE TABLE IF NOT EXISTS saldo_ajuste_teste (id SERIAL PRIMARY KEY, valor NUMERIC(15,2) NOT NULL, criado_em TIMESTAMP DEFAULT NOW())");
        } catch (SQLException ignored) {}
        String sql = "SELECT " +
                "(SELECT COALESCE(SUM(valor_aporte), 0) FROM aporte_investimento) + " +
                "(SELECT COALESCE(SUM(valor), 0) FROM saldo_ajuste_teste) - " +
                "(SELECT COALESCE(SUM(COALESCE(valor_pago, valor)), 0) FROM despesa WHERE data_pagamento IS NOT NULL) AS saldo";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal("saldo");
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao calcular saldo", e);
        }
        return BigDecimal.ZERO;
    }

    private Despesa extrair(ResultSet rs) throws SQLException {
        Despesa d = new Despesa();
        d.setId(rs.getInt("id"));
        d.setDescricao(rs.getString("descricao"));
        d.setValor(rs.getBigDecimal("valor"));
        d.setDataLancamento(rs.getObject("data_lancamento", LocalDate.class));
        d.setDataVencimento(rs.getObject("data_vencimento", LocalDate.class));
        d.setDataPagamento(rs.getObject("data_pagamento", LocalDate.class));
        d.setDataPrazo(rs.getObject("data_prazo", LocalDate.class));
        d.setValorPago(rs.getBigDecimal("valor_pago"));
        d.setCategoriaDespesaId(rs.getInt("categoria_despesa_id"));
        d.setCategoriaDespesaNome(rs.getString("tipo_nome"));
        d.setColaboradorLancouId((Integer) rs.getObject("colaborador_lancou_id"));
        return d;
    }
    public boolean estornar(Connection conn, int id) throws SQLException {
        String sql = "UPDATE despesa SET data_pagamento = NULL WHERE id = ? AND data_pagamento IS NOT NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao estornar despesa", e);
        }
    }

    public boolean ajustarSaldo(Connection conn, java.math.BigDecimal novoValor) throws SQLException {
        // Garante que a tabela existe
        try (java.sql.Statement s = conn.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS saldo_ajuste_teste (id SERIAL PRIMARY KEY, valor NUMERIC(15,2) NOT NULL, criado_em TIMESTAMP DEFAULT NOW())");
        } catch (SQLException ignored) {}
        // Remove ajustes anteriores e insere o novo valor
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM saldo_ajuste_teste")) {
            del.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao limpar ajuste de saldo", e);
        }
        String sql = "INSERT INTO saldo_ajuste_teste (valor) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, novoValor);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao ajustar saldo", e);
        }
    }


}