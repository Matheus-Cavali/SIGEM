package org.example.model;

import com.google.gson.Gson;
import org.example.dao.CategoriaEventoDao;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaEvento {

    private Integer id;
    private String nome;

    private static CategoriaEventoDao dao;

    public static synchronized CategoriaEventoDao getDao(){
        if(dao == null)
            dao = new CategoriaEventoDao();

        return dao;
    }

    public CategoriaEvento() {}

    public CategoriaEvento(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaEvento(String nome) {
        this(null, nome);
    }

    public static Map<String, String> validarCategoriaEvento(CategoriaEvento ce){
        Map<String, String> erros = new LinkedHashMap<>();

        if(ce.getNome() == null || ce.getNome().trim().isEmpty())
            erros.put("nome", "Nome da categoria é obrigatório");

        return erros;
    }

    public static String validarNomeDuplicado(Connection conn, String nome, Integer idAtual){
        CategoriaEvento existente = getDao().buscarPorNomeExato(conn, nome);

        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Categoria de evento já cadastrada anteriormente.";
        }

        return null;
    }

    public void cadastrar(Connection conn, CategoriaEvento ce){

        Map<String, String> erros = validarCategoriaEvento(ce);

        String nomeDuplicado = validarNomeDuplicado(conn, ce.getNome(), null);

        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);

        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        getDao().cadastrar(conn, ce);
    }

    public void alterar(Connection conn, CategoriaEvento ce){

        if(ce.getId() == null || ce.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Map<String, String> erros = validarCategoriaEvento(ce);

        String nomeDuplicado = validarNomeDuplicado(conn, ce.getNome(), ce.getId());

        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);

        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        getDao().atualizar(conn, ce);
    }

    public void excluir(Connection conn, Integer id){

        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        getDao().excluir(conn, id);
    }

    public CategoriaEvento buscarPorId(Connection conn, Integer id){

        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        return getDao().buscarPorId(conn, id);
    }

    public List<CategoriaEvento> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<CategoriaEvento> filtrar(Connection conn, String nome){
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