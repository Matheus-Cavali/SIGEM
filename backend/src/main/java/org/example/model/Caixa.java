package org.example.model;

import com.google.gson.Gson;
import org.example.dao.CaixaDao;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Caixa {
    private Integer id;
    private LocalDate dataCaixa;
    private LocalTime horarioAbertura;
    private LocalTime horarioFechamento;
    private BigDecimal valorAbertura;
    private BigDecimal valorFechamento;
    private BigDecimal saldo;
    private Integer colaboradorAbriuId;
    private Integer colaboradorFechouId;

    private static CaixaDao dao;

    public static synchronized CaixaDao getDao(){
        if(dao == null)
            dao = new CaixaDao();
        return dao;
    }

    public Caixa() {}

    public Caixa(Integer id, LocalDate dataCaixa, LocalTime horarioAbertura, LocalTime horarioFechamento, BigDecimal valorAbertura, BigDecimal valorFechamento, BigDecimal saldo, Integer colaboradorAbriuId, Integer colaboradorFechouId) {
        this.id = id;
        this.dataCaixa = dataCaixa;
        this.horarioAbertura = horarioAbertura;
        this.horarioFechamento = horarioFechamento;
        this.valorAbertura = valorAbertura;
        this.valorFechamento = valorFechamento;
        this.saldo = saldo;
        this.colaboradorAbriuId = colaboradorAbriuId;
        this.colaboradorFechouId = colaboradorFechouId;
    }

    public Caixa(LocalDate dataCaixa, LocalTime horarioAbertura, LocalTime horarioFechamento, BigDecimal valorAbertura, BigDecimal valorFechamento, BigDecimal saldo, Integer colaboradorAbriuId, Integer colaboradorFechouId) {
        this(null, dataCaixa, horarioAbertura, horarioFechamento, valorAbertura, valorFechamento, saldo, colaboradorAbriuId, colaboradorFechouId);
    }

    public static Map<String, String> validarFechamento(Caixa c){
        Map<String, String> erros = new LinkedHashMap<>();
        if(c.getValorFechamento() == null || c.getValorFechamento().compareTo(BigDecimal.ZERO) < 0)
            erros.put("valorFechamento", "Valor de fechamento é obrigatório e não pode ser negativo");
        return erros;
    }

    public void abrir(Connection conn, Caixa c){
        Caixa aberto = getDao().buscarCaixaAberto(conn);
        if(aberto != null){
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("dataCaixa", "Já existe um caixa aberto. Feche-o antes de abrir outro.");
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        }

        Caixa ultimo = getDao().buscarUltimoCaixa(conn);
        if(ultimo == null || ultimo.getValorFechamento() == null)
            c.setValorAbertura(BigDecimal.ZERO);
        else
            c.setValorAbertura(ultimo.getValorFechamento());

        c.setSaldo(c.getValorAbertura());
        c.setDataCaixa(LocalDate.now());
        c.setHorarioAbertura(LocalTime.now());

        getDao().inserir(conn, c);
    }

    public void fechar(Connection conn, Caixa c){
        Map<String, String> erros = validarFechamento(c);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        Caixa existente = getDao().buscarPorId(conn, c.getId());
        if(existente == null)
            throw new RuntimeException("Caixa não encontrado.");
        if(existente.getHorarioFechamento() != null)
            throw new RuntimeException("Caixa já foi fechado.");

        c.setHorarioFechamento(LocalTime.now());

        getDao().fechar(conn, c);
    }

    public void movimentar(Connection conn, int id, BigDecimal valor){
        Caixa existente = getDao().buscarPorId(conn, id);
        if(existente == null)
            throw new RuntimeException("Caixa não encontrado.");
        if(existente.getHorarioFechamento() != null)
            throw new RuntimeException("Caixa já está fechado, não é possível movimentar.");

        getDao().atualizarSaldo(conn, id, valor);
    }

    public Caixa buscarCaixaAberto(Connection conn){
        return getDao().buscarCaixaAberto(conn);
    }

    public Caixa buscarUltimoCaixa(Connection conn){
        return getDao().buscarUltimoCaixa(conn);
    }

    public Caixa buscarPorId(Connection conn, Integer id){
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        return getDao().buscarPorId(conn, id);
    }

    public List<Caixa> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDataCaixa() {
        return dataCaixa;
    }

    public void setDataCaixa(LocalDate dataCaixa) {
        this.dataCaixa = dataCaixa;
    }

    public LocalTime getHorarioAbertura() {
        return horarioAbertura;
    }

    public void setHorarioAbertura(LocalTime horarioAbertura) {
        this.horarioAbertura = horarioAbertura;
    }

    public LocalTime getHorarioFechamento() {
        return horarioFechamento;
    }

    public void setHorarioFechamento(LocalTime horarioFechamento) {
        this.horarioFechamento = horarioFechamento;
    }

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public Integer getColaboradorAbriuId() {
        return colaboradorAbriuId;
    }

    public void setColaboradorAbriuId(Integer colaboradorAbriuId) {
        this.colaboradorAbriuId = colaboradorAbriuId;
    }

    public Integer getColaboradorFechouId() {
        return colaboradorFechouId;
    }

    public void setColaboradorFechouId(Integer colaboradorFechouId) {
        this.colaboradorFechouId = colaboradorFechouId;
    }
}
