package org.example.model;

import com.google.gson.Gson;
import org.example.dao.DoacaoMaterialDao;
import org.example.dao.MaterialDao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;

public class DoacaoMaterial extends Doacao {
    private Integer materialId;
    private int quantidade;
    private String colaboradorNome;
    private String materialNome;
    private String dataFormatada;

    private static DoacaoMaterialDao dao;

    public static synchronized DoacaoMaterialDao getDao(){
        if(dao == null)
            dao = new DoacaoMaterialDao();
        return dao;
    }

    public DoacaoMaterial() {
        super();
    }

    public DoacaoMaterial(Integer id, LocalDate data, Integer colaboradorId, Integer materialId, int quantidade) {
        super(id, data, colaboradorId);
        this.materialId = materialId;
        this.quantidade = quantidade;
    }

    public DoacaoMaterial(LocalDate data, Integer colaboradorId, Integer materialId, int quantidade) {
        super(data, colaboradorId);
        this.materialId = materialId;
        this.quantidade = quantidade;
    }

    public static Map<String, String> validarDoacaoMaterial(DoacaoMaterial dm){
        Map<String, String> erros = new LinkedHashMap<>();
        if(dm.getMaterialId() == null || dm.getMaterialId() <= 0)
            erros.put("materialId", "Material é obrigatório");
        if(dm.getQuantidade() <= 0)
            erros.put("quantidade", "Quantidade deve ser maior que zero");
        if(dm.getColaboradorId() == null || dm.getColaboradorId() <= 0)
            erros.put("colaboradorId", "Colaborador é obrigatório");
        return erros;
    }

    public void cadastrar(Connection conn, DoacaoMaterial dm){
        Map<String, String> erros = validarDoacaoMaterial(dm);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        int id = getDao().inserirDoacao(conn, dm);
        dm.setId(id);

        getDao().inserirDoacaoMaterial(conn, dm);

        new MaterialDao().atualizarQuantidade(conn, dm.getMaterialId(), dm.getQuantidade());
    }

    public void excluir(Connection conn, int doacaoId){
        DoacaoMaterial dm = getDao().buscarPorDoacaoId(conn, doacaoId);
        if(dm == null)
            throw new RuntimeException("Doação de material não encontrada");

        Material material = new MaterialDao().buscarPorId(conn, dm.getMaterialId());
        if(material == null)
            throw new RuntimeException("Material não encontrado");

        int novoEstoque = material.getQuantidadeEstoque() - dm.getQuantidade();
        if(novoEstoque < 0)
            throw new RuntimeException("Quantidade de materiais em estoque insuficientes para realizar a exclusão");

        getDao().excluirDoacaoMaterial(conn, doacaoId);
        getDao().excluirDoacao(conn, doacaoId);

        new MaterialDao().atualizarQuantidade(conn, dm.getMaterialId(), -dm.getQuantidade());
    }

    public List<DoacaoMaterial> filtrar(Connection conn, String materialNome, Integer categoriaId, LocalDate dataInicio, LocalDate dataFim){
        return getDao().listar(conn, materialNome, categoriaId, dataInicio, dataFim);
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getColaboradorNome() {
        return colaboradorNome;
    }

    public void setColaboradorNome(String colaboradorNome) {
        this.colaboradorNome = colaboradorNome;
    }

    public String getMaterialNome() {
        return materialNome;
    }

    public void setMaterialNome(String materialNome) {
        this.materialNome = materialNome;
    }

    public String getDataFormatada() {
        return dataFormatada;
    }

    public void setDataFormatada(String dataFormatada) {
        this.dataFormatada = dataFormatada;
    }
}
