package org.example.model;

import com.google.gson.Gson;
import org.example.dao.DoacaoFinanceiraDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DoacaoFinanceira extends Doacao {
    private BigDecimal valor;
    private Integer categoriaFinanceiraId;
    private Integer caixaId;
    private String categoriaFinanceiraNome;
    private String colaboradorNome;
    private String dataFormatada;

    private static DoacaoFinanceiraDao dao;

    public static synchronized DoacaoFinanceiraDao getDao(){
        if(dao == null)
            dao = new DoacaoFinanceiraDao();
        return dao;
    }

    public DoacaoFinanceira() { super(); }

    public DoacaoFinanceira(Integer id, LocalDate data, Integer colaboradorId, String colaboradorNome,
                            BigDecimal valor, Integer categoriaFinanceiraId, String categoriaFinanceiraNome, Integer caixaId) {
        super(id, data, colaboradorId);
        this.valor = valor;
        this.categoriaFinanceiraId = categoriaFinanceiraId;
        this.caixaId = caixaId;
        this.colaboradorNome = colaboradorNome;
        this.categoriaFinanceiraNome = categoriaFinanceiraNome;
        this.dataFormatada = data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }

    public DoacaoFinanceira(LocalDate data, Integer colaboradorId, BigDecimal valor,
                            Integer categoriaFinanceiraId, String colaboradorNome, String categoriaFinanceiraNome) {
        super(data, colaboradorId);
        this.valor = valor;
        this.categoriaFinanceiraId = categoriaFinanceiraId;
        this.colaboradorNome = colaboradorNome;
        this.categoriaFinanceiraNome = categoriaFinanceiraNome;
        this.dataFormatada = data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }

    public static Map<String, String> validarDoacaoFinanceira(DoacaoFinanceira df){
        Map<String, String> erros = new LinkedHashMap<>();
        if(df.getValor() == null || df.getValor().compareTo(BigDecimal.ZERO) <= 0)
            erros.put("valor", "Valor deve ser maior que zero");
        if(df.getCategoriaFinanceiraId() == null || df.getCategoriaFinanceiraId() <= 0)
            erros.put("categoriaFinanceiraId", "Categoria financeira é obrigatória");
        if(df.getColaboradorId() == null || df.getColaboradorId() <= 0)
            erros.put("colaboradorId", "Colaborador é obrigatório");
        return erros;
    }

    public void cadastrar(Connection conn, DoacaoFinanceira df){
        Map<String, String> erros = validarDoacaoFinanceira(df);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        Caixa caixaAberto = new Caixa().buscarCaixaAberto(conn);
        if(caixaAberto == null){
            Map<String, String> err = new LinkedHashMap<>();
            err.put("caixa", "Caixa fechado. Abra o caixa antes de registrar doação financeira.");
            throw new RuntimeException(new Gson().toJson(Map.of("erros", err)));
        }
        df.setCaixaId(caixaAberto.getId());

        int id = getDao().inserirDoacao(conn, df);
        df.setId(id);

        getDao().inserirDoacaoFinanceira(conn, df);

        new Caixa().movimentar(conn, caixaAberto.getId(), df.getValor());
    }

    public void alterar(Connection conn, DoacaoFinanceira novosDados){
        Map<String, String> erros = validarDoacaoFinanceira(novosDados);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        DoacaoFinanceira original = getDao().buscarPorDoacaoId(conn, novosDados.getId());
        if(original == null)
            throw new RuntimeException("Doação financeira não encontrada");

        novosDados.setColaboradorId(original.getColaboradorId());
        if(novosDados.getData() == null)
            novosDados.setData(original.getData());

        novosDados.setCaixaId(original.getCaixaId());

        BigDecimal diff = novosDados.getValor().subtract(original.getValor());

        new Caixa().movimentar(conn, original.getCaixaId(), diff);

        getDao().atualizar(conn, novosDados);
    }

    public void excluir(Connection conn, int doacaoId){
        DoacaoFinanceira df = getDao().buscarPorDoacaoId(conn, doacaoId);
        if(df == null)
            throw new RuntimeException("Doação financeira não encontrada");

        new Caixa().movimentar(conn, df.getCaixaId(), df.getValor().negate());

        getDao().excluirDoacaoFinanceira(conn, doacaoId);
        getDao().excluirDoacao(conn, doacaoId);
    }

    public List<DoacaoFinanceira> filtrar(Connection conn, Integer categoriaId, LocalDate dataInicio, LocalDate dataFim){
        return getDao().listar(conn, categoriaId, dataInicio, dataFim);
    }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public Integer getCategoriaFinanceiraId() { return categoriaFinanceiraId; }
    public void setCategoriaFinanceiraId(Integer categoriaFinanceiraId) { this.categoriaFinanceiraId = categoriaFinanceiraId; }
    public Integer getCaixaId() { return caixaId; }
    public void setCaixaId(Integer caixaId) { this.caixaId = caixaId; }
    public String getCategoriaFinanceiraNome() { return categoriaFinanceiraNome; }
    public void setCategoriaFinanceiraNome(String categoriaFinanceiraNome) { this.categoriaFinanceiraNome = categoriaFinanceiraNome; }
    public String getColaboradorNome() { return colaboradorNome; }
    public void setColaboradorNome(String colaboradorNome) { this.colaboradorNome = colaboradorNome; }
    public String getDataFormatada() { return dataFormatada; }
    public void setDataFormatada(String dataFormatada) { this.dataFormatada = dataFormatada; }
}