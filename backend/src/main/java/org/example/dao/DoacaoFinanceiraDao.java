package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.DoacaoFinanceira;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DoacaoFinanceiraDao {

    public int inserirDoacao(Connection conn, DoacaoFinanceira df){
        String sql = "INSERT INTO doacao (data_doacao, colaborador_id) VALUES (?, ?) RETURNING id";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setObject(1, df.getData());
            stmt.setInt(2, df.getColaboradorId());
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()) return rs.getInt("id");
                throw new DatabaseException("Erro ao cadastrar doacao: nenhum ID retornado");
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar doacao", e);
        }
    }

    public void inserirDoacaoFinanceira(Connection conn, DoacaoFinanceira df){
        String sql = "INSERT INTO doacao_financeira (doacao_id, valor, categoria_financeira_id, caixa_id) VALUES (?, ?, ?, ?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, df.getId());
            stmt.setBigDecimal(2, df.getValor());
            stmt.setInt(3, df.getCategoriaFinanceiraId());
            stmt.setInt(4, df.getCaixaId());
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar doacao financeira", e);
        }
    }

    public List<DoacaoFinanceira> listar(Connection conn, Integer categoriaId, LocalDate dataInicio, LocalDate dataFim){
        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.data_doacao, d.colaborador_id, u.nome AS colaborador_nome, " +
            "df.valor, df.categoria_financeira_id, cf.nome AS categoria_nome, df.caixa_id " +
            "FROM doacao d " +
            "JOIN doacao_financeira df ON d.id = df.doacao_id " +
            "JOIN colaborador c ON d.colaborador_id = c.usuario_id " +
            "JOIN usuario u ON c.usuario_id = u.id " +
            "JOIN categoria_financeira cf ON df.categoria_financeira_id = cf.id " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if(categoriaId != null){
            sql.append(" AND df.categoria_financeira_id = ?");
            params.add(categoriaId);
        }
        if(dataInicio != null){
            sql.append(" AND d.data_doacao >= ?");
            params.add(dataInicio);
        }
        if(dataFim != null){
            sql.append(" AND d.data_doacao <= ?");
            params.add(dataFim);
        }

        sql.append(" ORDER BY d.data_doacao DESC, d.id DESC");

        List<DoacaoFinanceira> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())){
            for(int i = 0; i < params.size(); i++){
                Object param = params.get(i);
                if(param instanceof Integer)
                    stmt.setInt(i + 1, (Integer) param);
                else if(param instanceof LocalDate)
                    stmt.setObject(i + 1, param);
            }
            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao listar doacoes financeiras", e);
        }
        return lista;
    }

    public DoacaoFinanceira buscarPorDoacaoId(Connection conn, int doacaoId){
        String sql = "SELECT d.id, d.data_doacao, d.colaborador_id, df.valor, df.categoria_financeira_id, df.caixa_id " +
                     "FROM doacao d " +
                     "JOIN doacao_financeira df ON d.id = df.doacao_id " +
                     "WHERE d.id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    DoacaoFinanceira df = new DoacaoFinanceira();
                    df.setId(rs.getInt("id"));
                    df.setData(rs.getObject("data_doacao", LocalDate.class));
                    df.setColaboradorId(rs.getInt("colaborador_id"));
                    df.setValor(rs.getBigDecimal("valor"));
                    df.setCategoriaFinanceiraId(rs.getInt("categoria_financeira_id"));
                    df.setCaixaId(rs.getInt("caixa_id"));
                    return df;
                }
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao buscar doacao financeira", e);
        }
        return null;
    }

    public void atualizar(Connection conn, DoacaoFinanceira df){
        String sqlDoacao = "UPDATE doacao SET data_doacao = ?, colaborador_id = ? WHERE id = ?";
        String sqlDoacaoFinanceira = "UPDATE doacao_financeira SET valor = ?, categoria_financeira_id = ? WHERE doacao_id = ?";
        try{
            try(PreparedStatement stmt = conn.prepareStatement(sqlDoacao)){
                stmt.setObject(1, df.getData());
                stmt.setInt(2, df.getColaboradorId());
                stmt.setInt(3, df.getId());
                stmt.executeUpdate();
            }
            try(PreparedStatement stmt = conn.prepareStatement(sqlDoacaoFinanceira)){
                stmt.setBigDecimal(1, df.getValor());
                stmt.setInt(2, df.getCategoriaFinanceiraId());
                stmt.setInt(3, df.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar doacao financeira", e);
        }
    }

    public void excluirDoacaoFinanceira(Connection conn, int doacaoId){
        String sql = "DELETE FROM doacao_financeira WHERE doacao_id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DatabaseException("Erro ao excluir doacao financeira", e);
        }
    }

    public void excluirDoacao(Connection conn, int doacaoId){
        String sql = "DELETE FROM doacao WHERE id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);
            stmt.executeUpdate();
        } catch (SQLException e){
            throw new DatabaseException("Erro ao excluir doacao", e);
        }
    }

    private DoacaoFinanceira extrair(ResultSet rs) throws SQLException{
        return new DoacaoFinanceira(
            rs.getInt("id"),
            rs.getObject("data_doacao", LocalDate.class),
            rs.getInt("colaborador_id"),
            rs.getString("colaborador_nome"),
            rs.getBigDecimal("valor"),
            rs.getInt("categoria_financeira_id"),
            rs.getString("categoria_nome"),
            rs.getInt("caixa_id")
        );
    }
}
