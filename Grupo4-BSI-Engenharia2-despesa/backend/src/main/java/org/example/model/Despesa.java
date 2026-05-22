package org.example.model;

import org.example.dao.DespesaDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Despesa {
    private int id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataLancamento;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private int categoriaDespesaId;
    private String categoriaDespesaNome;
    private Integer colaboradorLancouId;

    public Despesa() {}

    public static int inserir(Connection conn, Despesa d) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.inserir(conn, d);
    }

    public static boolean atualizar(Connection conn, Despesa d) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.atualizar(conn, d);
    }

    public static boolean deletar(Connection conn, int id) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.deletar(conn, id);
    }

    public static boolean quitar(Connection conn, int id, LocalDate dataPagamento) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.quitar(conn, id, dataPagamento);
    }

    public static java.math.BigDecimal calcularSaldo(Connection conn) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.calcularSaldo(conn);
    }

    public static Despesa buscarPorId(Connection conn, int id) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.buscarPorId(conn, id);
    }

    public static List<Despesa> listar(Connection conn, String descricao, Integer categoriaId) throws SQLException {
        DespesaDao dao = new DespesaDao();
        return dao.listar(conn, descricao, categoriaId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataLancamento() { return dataLancamento; }
    public void setDataLancamento(LocalDate dataLancamento) { this.dataLancamento = dataLancamento; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public int getCategoriaDespesaId() { return categoriaDespesaId; }
    public void setCategoriaDespesaId(int categoriaDespesaId) { this.categoriaDespesaId = categoriaDespesaId; }

    public String getCategoriaDespesaNome() { return categoriaDespesaNome; }
    public void setCategoriaDespesaNome(String categoriaDespesaNome) { this.categoriaDespesaNome = categoriaDespesaNome; }

    public Integer getColaboradorLancouId() { return colaboradorLancouId; }
    public void setColaboradorLancouId(Integer colaboradorLancouId) { this.colaboradorLancouId = colaboradorLancouId; }
}
