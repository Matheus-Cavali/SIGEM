package org.example.model;

import com.google.gson.Gson;
import org.example.dao.EventoDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Evento {

    private Integer id;
    private String nome;
    private String descricao;
    private java.sql.Date dataRegistro;
    private Timestamp dataInicio;
    private Timestamp dataFim;
    private String status;
    private BigDecimal resultadoFinanceiro;
    private String observacoesHistorico;
    private Integer localId;
    private Integer categoriaEventoId;
    private Integer colaboradorCadastrouId;
    private Integer coordenadorId;

    private static EventoDao dao;

    public static synchronized EventoDao getDao() {
        if (dao == null)
            dao = new EventoDao();

        return dao;
    }

    public Evento() {}

    public Evento(Integer id,
                  String nome,
                  String descricao,
                  java.sql.Date dataRegistro,
                  Timestamp dataInicio,
                  Timestamp dataFim,
                  String status,
                  BigDecimal resultadoFinanceiro,
                  String observacoesHistorico,
                  Integer localId,
                  Integer categoriaEventoId,
                  Integer colaboradorCadastrouId,
                  Integer coordenadorId) {

        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.dataRegistro = dataRegistro;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
        this.resultadoFinanceiro = resultadoFinanceiro;
        this.observacoesHistorico = observacoesHistorico;
        this.localId = localId;
        this.categoriaEventoId = categoriaEventoId;
        this.colaboradorCadastrouId = colaboradorCadastrouId;
        this.coordenadorId = coordenadorId;
    }

    public static Map<String, String> validarEvento(Evento evento) {

        Map<String, String> erros = new LinkedHashMap<>();

        if (evento.getNome() == null || evento.getNome().trim().isEmpty())
            erros.put("nome", "Nome do evento é obrigatório");

        if (evento.getDataInicio() == null)
            erros.put("dataInicio", "Data de início é obrigatória");

        if (evento.getDataFim() == null)
            erros.put("dataFim", "Data de fim é obrigatória");

    if (evento.getDataInicio() != null &&
            evento.getDataFim() != null &&
            evento.getDataFim().before(evento.getDataInicio())) {

            erros.put("dataFim", "Data de fim não pode ser menor que a data de início");
        }

        if (evento.getDataInicio() != null &&
                evento.getDataInicio().toLocalDateTime().toLocalDate()
                        .isBefore(java.time.LocalDate.now())) {

            erros.put("dataInicio", "Data de início não pode ser no passado");
        }

        return erros;
    }

    public void cadastrar(Connection conn, Evento evento) {

        Map<String, String> erros = validarEvento(evento);

        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        evento.setStatus("AGENDADO");

        getDao().cadastrar(conn, evento);
    }

    public void alterar(Connection conn, Evento evento) {

        if (evento.getId() == null || evento.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Evento atual = getDao().buscarPorId(conn, evento.getId());

        if (atual == null)
            throw new IllegalArgumentException("Evento não encontrado.");

        if (!"AGENDADO".equals(atual.getStatus()))
            throw new IllegalArgumentException("Somente eventos agendados podem ser alterados.");

        Map<String, String> erros = validarEvento(evento);

        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        getDao().atualizar(conn, evento);
    }

    public void abrir(Connection conn, Integer id) {

        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Evento evento = getDao().buscarPorId(conn, id);

        if (evento == null)
            throw new IllegalArgumentException("Evento não encontrado.");

        if (!"AGENDADO".equals(evento.getStatus()))
            throw new IllegalArgumentException("Somente eventos agendados podem ser abertos.");

        if (evento.getCoordenadorId() == null)
            throw new IllegalArgumentException("Não é possível abrir um evento sem coordenador.");

        if (!evento.getDataInicio().toLocalDateTime().toLocalDate().equals(LocalDate.now()))
            throw new IllegalArgumentException("Só é possível abrir o evento na data agendada.");

        getDao().abrirEvento(conn, id, new Timestamp(System.currentTimeMillis()));
    }

    public void cancelar(Connection conn, Integer id) {

        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Evento evento = getDao().buscarPorId(conn, id);

        if (evento == null)
            throw new IllegalArgumentException("Evento não encontrado.");

        if (!"AGENDADO".equals(evento.getStatus()))
            throw new IllegalArgumentException("Somente eventos agendados podem ser cancelados.");

        getDao().alterarStatus(conn, id, "CANCELADO");
    }

    public void encerrar(Connection conn,
                         Integer id,
                         BigDecimal resultadoFinanceiro,
                         String observacoesHistorico) {

        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Evento evento = getDao().buscarPorId(conn, id);

        if (evento == null)
            throw new IllegalArgumentException("Evento não encontrado.");

        if (!"ABERTO".equals(evento.getStatus()))
            throw new IllegalArgumentException("Somente eventos abertos podem ser encerrados.");

        if (resultadoFinanceiro == null)
            throw new IllegalArgumentException("Resultado financeiro é obrigatório.");

        getDao().encerrarEvento(conn,
                id,
                new Timestamp(System.currentTimeMillis()),
                observacoesHistorico,
                resultadoFinanceiro);
    }

    public void excluir(Connection conn, Integer id) {

        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Evento evento = getDao().buscarPorId(conn, id);

        if (evento == null)
            throw new IllegalArgumentException("Evento não encontrado.");

        if (!"AGENDADO".equals(evento.getStatus()) &&
                !"CANCELADO".equals(evento.getStatus())) {

            throw new IllegalArgumentException(
                    "Somente eventos agendados ou cancelados podem ser excluídos."
            );
        }

        getDao().excluir(conn, id);
    }

    public Evento buscarPorId(Connection conn, Integer id) {

        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        return getDao().buscarPorId(conn, id);
    }

    public List<Evento> listar(Connection conn,
                               String nome,
                               String status) {

        return getDao().listar(conn, nome, status);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public java.sql.Date getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(java.sql.Date dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public Timestamp getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Timestamp dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Timestamp getDataFim() {
        return dataFim;
    }

    public void setDataFim(Timestamp dataFim) {
        this.dataFim = dataFim;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getResultadoFinanceiro() {
        return resultadoFinanceiro;
    }

    public void setResultadoFinanceiro(BigDecimal resultadoFinanceiro) {
        this.resultadoFinanceiro = resultadoFinanceiro;
    }

    public String getObservacoesHistorico() {
        return observacoesHistorico;
    }

    public void setObservacoesHistorico(String observacoesHistorico) {
        this.observacoesHistorico = observacoesHistorico;
    }

    public Integer getLocalId() {
        return localId;
    }

    public void setLocalId(Integer localId) {
        this.localId = localId;
    }

    public Integer getCategoriaEventoId() {
        return categoriaEventoId;
    }

    public void setCategoriaEventoId(Integer categoriaEventoId) {
        this.categoriaEventoId = categoriaEventoId;
    }

    public Integer getColaboradorCadastrouId() {
        return colaboradorCadastrouId;
    }

    public void setColaboradorCadastrouId(Integer colaboradorCadastrouId) {
        this.colaboradorCadastrouId = colaboradorCadastrouId;
    }

    public Integer getCoordenadorId() {
        return coordenadorId;
    }

    public void setCoordenadorId(Integer coordenadorId) {
        this.coordenadorId = coordenadorId;
    }
}