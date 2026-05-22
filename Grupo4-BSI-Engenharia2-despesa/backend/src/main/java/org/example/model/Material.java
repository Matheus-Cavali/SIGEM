package org.example.model;

import com.google.gson.Gson;
import org.example.dao.MaterialDao;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Material {
    private Integer id;
    private String nome;
    private String descricao;
    private int quantidadeEstoque;
    private Integer categoriaMaterialId;

    private static MaterialDao dao;

    public static synchronized MaterialDao getDao(){
        if(dao == null)
            dao = new MaterialDao();

        return dao;
    }

    public Material() {}

    public Material(Integer id, String nome, String descricao, int quantidadeEstoque, Integer categoriaMaterialId) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.quantidadeEstoque = quantidadeEstoque;
        this.categoriaMaterialId = categoriaMaterialId;
    }

    public Material(String nome, String descricao, int quantidadeEstoque, Integer categoriaMaterialId) {
        this(null, nome, descricao, quantidadeEstoque, categoriaMaterialId);
    }

    public static Map<String, String> validarMaterial(Material m){
        Map<String, String> erros = new LinkedHashMap<>();
        if(m.getNome() == null || m.getNome().trim().isEmpty())
            erros.put("nome", "Nome do material é obrigatório");
        if(m.getDescricao() == null || m.getDescricao().trim().isEmpty())
            erros.put("descricao", "Descrição do material é obrigatória");
        if(m.getQuantidadeEstoque() < 0)
            erros.put("quantidadeEstoque", "Quantidade em estoque não pode ser negativa");
        if(m.getCategoriaMaterialId() == null || m.getCategoriaMaterialId() <= 0)
            erros.put("categoriaMaterialId", "Categoria é obrigatória");
        return erros;
    }

    public static String validarNomeDuplicado(Connection conn, String nome, Integer idAtual){
        Material existente = getDao().buscarPorNomeExato(conn, nome);
        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Material já cadastrado anteriormente.";
        }
        return null;
    }

    public void cadastrar(Connection conn, Material m){
        Map<String, String> erros = validarMaterial(m);
        String nomeDuplicado = validarNomeDuplicado(conn, m.getNome(), null);
        if(nomeDuplicado != null) erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty()) throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().cadastrar(conn, m);
    }

    public void alterar(Connection conn, Material m){
        if(m.getId() == null || m.getId() <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        Map<String, String> erros = validarMaterial(m);
        String nomeDuplicado = validarNomeDuplicado(conn, m.getNome(), m.getId());
        if(nomeDuplicado != null) erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty()) throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, m);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        getDao().excluir(conn, id);
    }

    public Material buscarPorId(Connection conn, Integer id) {
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return getDao().buscarPorId(conn, id);
    }

    public List<Material> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<Material> filtrar(Connection conn, String nome, Integer categoriaId){
        return getDao().listar(conn, nome, categoriaId);
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

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Integer getCategoriaMaterialId() {
        return categoriaMaterialId;
    }

    public void setCategoriaMaterialId(Integer categoriaMaterialId) {
        this.categoriaMaterialId = categoriaMaterialId;
    }
}
