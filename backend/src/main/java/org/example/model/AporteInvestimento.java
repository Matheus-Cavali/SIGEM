package org.example.model;

import com.google.gson.Gson;
import org.example.dao.AporteInvestimentoDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AporteInvestimento {
    private int id;
    private int investimentoFuturoId;
    private BigDecimal valorAporte;
    private LocalDate dataAporte;
    private Integer caixaId;
    private Integer colaboradorId;
    private String colaboradorNome;

    private static AporteInvestimentoDao dao;

    public static synchronized AporteInvestimentoDao getAporteInvestimentoDao() {
        if (dao == null) dao = new AporteInvestimentoDao();
        return dao;
    }

    public AporteInvestimento() {}

    public Map<String, String> validar() {
        Map<String, String> erros = new LinkedHashMap<>();
        if (valorAporte == null || valorAporte.compareTo(BigDecimal.ZERO) <= 0)
            erros.put("valorAporte", "Valor do aporte deve ser maior que zero");
        if (dataAporte == null)
            erros.put("dataAporte", "Data do aporte é obrigatória");
        return erros;
    }

    public void cadastrar(Connection conn, AporteInvestimento aporte) {
        Map<String, String> erros = aporte.validar();
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        int id = getAporteInvestimentoDao().inserir(conn, aporte);
        aporte.setId(id);
    }

    public void alterar(Connection conn, int id, AporteInvestimento aporte) {
        Map<String, String> erros = aporte.validar();
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getAporteInvestimentoDao().atualizar(conn, id, aporte);
    }

    public void excluir(Connection conn, int id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        getAporteInvestimentoDao().deletar(conn, id);
    }

    public void excluirPorInvestimento(Connection conn, int investimentoId) {
        getAporteInvestimentoDao().deletarPorInvestimento(conn, investimentoId);
    }

    public List<AporteInvestimento> filtrarPorInvestimento(Connection conn, int investimentoId) {
        return getAporteInvestimentoDao().listarPorInvestimento(conn, investimentoId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getInvestimentoFuturoId() { return investimentoFuturoId; }
    public void setInvestimentoFuturoId(int investimentoFuturoId) { this.investimentoFuturoId = investimentoFuturoId; }
    public BigDecimal getValorAporte() { return valorAporte; }
    public void setValorAporte(BigDecimal valorAporte) { this.valorAporte = valorAporte; }
    public LocalDate getDataAporte() { return dataAporte; }
    public void setDataAporte(LocalDate dataAporte) { this.dataAporte = dataAporte; }
    public Integer getCaixaId() { return caixaId; }
    public void setCaixaId(Integer caixaId) { this.caixaId = caixaId; }
    public Integer getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(Integer colaboradorId) { this.colaboradorId = colaboradorId; }
    public String getColaboradorNome() { return colaboradorNome; }
    public void setColaboradorNome(String colaboradorNome) { this.colaboradorNome = colaboradorNome; }
}
