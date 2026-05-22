package org.example.model;

import com.google.gson.Gson;
import org.example.dao.DespesaDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // ── Singleton do DAO (padrão Material) ────────────────────────────────────
    private static DespesaDao dao;

    public static synchronized DespesaDao getDao() {
        if (dao == null)
            dao = new DespesaDao();
        return dao;
    }

    public Despesa() {}

    // ── Validação por campo com Map de erros (padrão Material) ─────────────────
    public static Map<String, String> validarDespesa(Despesa d) {
        Map<String, String> erros = new LinkedHashMap<>();
        if (d.getDescricao() == null || d.getDescricao().trim().isEmpty())
            erros.put("descricao", "Descrição da despesa é obrigatória");
        if (d.getValor() == null || d.getValor().compareTo(BigDecimal.ZERO) <= 0)
            erros.put("valor", "Valor da despesa deve ser maior que zero");
        if (d.getDataVencimento() == null)
            erros.put("dataVencimento", "Data de vencimento é obrigatória");
        if (d.getCategoriaDespesaId() <= 0)
            erros.put("categoriaDespesaId", "Categoria é obrigatória");
        return erros;
    }

    // ── Métodos de instância com validação (padrão Material) ──────────────────
    public void cadastrar(Connection conn, Despesa d) throws SQLException {
        Map<String, String> erros = validarDespesa(d);
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        if (d.getDataLancamento() == null)
            d.setDataLancamento(LocalDate.now());
        getDao().inserir(conn, d);
    }

    public void alterar(Connection conn, Despesa d) throws SQLException {
        if (d.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");
        Map<String, String> erros = validarDespesa(d);
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, d);
    }

    public void excluir(Connection conn, int id) throws SQLException {
        if (id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        getDao().deletar(conn, id);
    }

    // ── Métodos estáticos de acesso ao DAO (mantidos para compatibilidade) ────
    public static int inserir(Connection conn, Despesa d) throws SQLException {
        return getDao().inserir(conn, d);
    }

    public static boolean atualizar(Connection conn, Despesa d) throws SQLException {
        return getDao().atualizar(conn, d);
    }

    public static boolean deletar(Connection conn, int id) throws SQLException {
        return getDao().deletar(conn, id);
    }

    public static boolean quitar(Connection conn, int id, LocalDate dataPagamento) throws SQLException {
        return getDao().quitar(conn, id, dataPagamento);
    }

    public static BigDecimal calcularSaldo(Connection conn) throws SQLException {
        return getDao().calcularSaldo(conn);
    }

    public static Despesa buscarPorId(Connection conn, int id) throws SQLException {
        return getDao().buscarPorId(conn, id);
    }

    public static List<Despesa> listar(Connection conn, String descricao, Integer categoriaId) throws SQLException {
        return getDao().listar(conn, descricao, categoriaId);
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────
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
