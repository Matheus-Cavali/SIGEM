package org.example.model;

import com.google.gson.Gson;
import org.example.dao.CategoriaFinanceiraDao;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaFinanceira {
    private Integer id;
    private String nome;

    private static CategoriaFinanceiraDao dao;

    public static synchronized CategoriaFinanceiraDao getDao(){
        if(dao == null)
            dao = new CategoriaFinanceiraDao();
        return dao;
    }

    public CategoriaFinanceira() {}

    public CategoriaFinanceira(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaFinanceira(String nome) {
        this(null, nome);
    }

    public static Map<String, String> validarCategoriaFinanceira(CategoriaFinanceira cf){
        Map<String, String> erros = new LinkedHashMap<>();
        if(cf.getNome() == null || cf.getNome().trim().isEmpty())
            erros.put("nome", "Nome da categoria é obrigatório");
        return erros;
    }

    public static String validarNomeDuplicado(Connection conn, String nome, Integer idAtual){
        CategoriaFinanceira existente = getDao().buscarPorNomeExato(conn, nome);
        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Categoria financeira já cadastrada anteriormente.";
        }
        return null;
    }

    public void cadastrar(Connection conn, CategoriaFinanceira cf){
        Map<String, String> erros = validarCategoriaFinanceira(cf);
        String nomeDuplicado = validarNomeDuplicado(conn, cf.getNome(), null);
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().cadastrar(conn, cf);
    }

    public void alterar(Connection conn, CategoriaFinanceira cf){
        if(cf.getId() == null || cf.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");
        Map<String, String> erros = validarCategoriaFinanceira(cf);
        String nomeDuplicado = validarNomeDuplicado(conn, cf.getNome(), cf.getId());
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, cf);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        getDao().excluir(conn, id);
    }

    public CategoriaFinanceira buscarPorId(Connection conn, Integer id) {
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        return getDao().buscarPorId(conn, id);
    }

    public List<CategoriaFinanceira> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<CategoriaFinanceira> filtrar(Connection conn, String nome){
        return getDao().listar(conn, nome);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
