package org.example.model;

import com.google.gson.Gson;
import org.example.dao.CategoriaMaterialDao;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaMaterial {
    private Integer id;
    private String nome;

    private static CategoriaMaterialDao dao;

    public static synchronized CategoriaMaterialDao getDao(){
        if(dao == null)
            dao = new CategoriaMaterialDao();

        return dao;
    }

    public CategoriaMaterial() {}

    public CategoriaMaterial(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaMaterial(String nome) {
        this(null, nome);
    }

    public static Map<String, String> validarCategoriaMaterial(CategoriaMaterial cm){
        Map<String, String> erros = new LinkedHashMap<>();
        if(cm.getNome() == null || cm.getNome().trim().isEmpty())
            erros.put("nome", "Nome da categoria é obrigatório");
        return erros;
    }

    public static String validarNomeDuplicado(Connection conn, String nome, Integer idAtual){
        CategoriaMaterial existente = getDao().buscarPorNomeExato(conn, nome);
        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Categoria de material já cadastrada anteriormente.";
        }
        return null;
    }

    public void cadastrar(Connection conn, CategoriaMaterial cm){
        Map<String, String> erros = validarCategoriaMaterial(cm);
        String nomeDuplicado = validarNomeDuplicado(conn, cm.getNome(), null);
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().cadastrar(conn, cm);
    }

    public void alterar(Connection conn, CategoriaMaterial cm){
        if(cm.getId() == null || cm.getId() <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        Map<String, String> erros = validarCategoriaMaterial(cm);
        String nomeDuplicado = validarNomeDuplicado(conn, cm.getNome(), cm.getId());
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, cm);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        getDao().excluir(conn, id);
    }

    public CategoriaMaterial buscarPorId(Connection conn, Integer id) {
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return getDao().buscarPorId(conn, id);
    }

    public List<CategoriaMaterial> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<CategoriaMaterial> filtrar(Connection conn, String nome){
        return getDao().listar(conn, nome);
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
}
