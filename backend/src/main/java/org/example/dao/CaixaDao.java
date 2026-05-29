package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Caixa;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CaixaDao {

    public void inserir(Connection conn, Caixa c){
        String sql = "INSERT INTO caixa (data_caixa, horario_abertura, valor_abertura, saldo, colaborador_abriu_id) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setObject(1, c.getDataCaixa());
            stmt.setObject(2, c.getHorarioAbertura());
            stmt.setBigDecimal(3, c.getValorAbertura());
            stmt.setBigDecimal(4, c.getSaldo());
            stmt.setInt(5, c.getColaboradorAbriuId());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    c.setId(rs.getInt("id"));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao abrir caixa", e);
        }
    }

    public void fechar(Connection conn, Caixa c){
        String sql = "UPDATE caixa SET horario_fechamento = ?, valor_fechamento = ?, colaborador_fechou_id = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setObject(1, c.getHorarioFechamento());
            stmt.setBigDecimal(2, c.getValorFechamento());
            stmt.setInt(3, c.getColaboradorFechouId());
            stmt.setInt(4, c.getId());

            stmt.executeUpdate();
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao fechar caixa", e);
        }
    }

    public void atualizarSaldo(Connection conn, int id, BigDecimal valor){
        String sql = "UPDATE caixa SET saldo = saldo + ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setBigDecimal(1, valor);
            stmt.setInt(2, id);

            stmt.executeUpdate();
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao movimentar caixa", e);
        }
    }

    public Caixa buscarUltimoCaixa(Connection conn){
        String sql = "SELECT * FROM caixa ORDER BY id DESC LIMIT 1";

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            if(rs.next())
                return extrair(rs);
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar último caixa", e);
        }

        return null;
    }

    public Caixa buscarPorData(Connection conn, LocalDate data){
        String sql = "SELECT * FROM caixa WHERE data_caixa = ? ORDER BY id DESC LIMIT 1";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setObject(1, data);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar caixa por data", e);
        }

        return null;
    }

    public Caixa buscarCaixaAberto(Connection conn){
        String sql = "SELECT * FROM caixa WHERE horario_fechamento IS NULL ORDER BY id DESC LIMIT 1";

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            if(rs.next())
                return extrair(rs);
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar caixa aberto", e);
        }

        return null;
    }

    public Caixa buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM caixa WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar caixa", e);
        }

        return null;
    }

    public List<Caixa> listarTodos(Connection conn){
        String sql = "SELECT * FROM caixa ORDER BY data_caixa DESC, id DESC";
        List<Caixa> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar caixas", e);
        }

        return lista;
    }

    private Caixa extrair(ResultSet rs) throws SQLException{
        return new Caixa(
            rs.getInt("id"),
            rs.getObject("data_caixa", LocalDate.class),
            rs.getObject("horario_abertura", LocalTime.class),
            rs.getObject("horario_fechamento", LocalTime.class),
            rs.getBigDecimal("valor_abertura"),
            rs.getBigDecimal("valor_fechamento"),
            rs.getBigDecimal("saldo"),
            rs.getInt("colaborador_abriu_id"),
            rs.getObject("colaborador_fechou_id", Integer.class)
        );
    }
}
