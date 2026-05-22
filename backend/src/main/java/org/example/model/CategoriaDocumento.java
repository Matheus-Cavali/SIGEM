package org.example.model;

import com.google.gson.Gson;
import org.example.dao.CategoriaDocumentoDao;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaDocumento {
    private Integer id;
    private String nome;

    private static CategoriaDocumentoDao dao;

    public static synchronized CategoriaDocumentoDao getDao(){
        if(dao == null)
            dao = new CategoriaDocumentoDao();

        return dao;
    }

    public CategoriaDocumento() {}

    public CategoriaDocumento(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaDocumento(String nome) {
        this(null, nome);
    }

    public static Map<String, String> validarCategoriaDocumento(CategoriaDocumento cd){
        Map<String, String> erros = new LinkedHashMap<>();
        if(cd.getNome() == null || cd.getNome().trim().isEmpty())
            erros.put("nome", "Nome da categoria é obrigatório");
        return erros;
    }

    public static String validarNomeDuplicado(Connection conn, String nome, Integer idAtual){
        CategoriaDocumento existente = getDao().buscarPorNomeExato(conn, nome);
        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Categoria de documento já cadastrada anteriormente.";
        }
        return null;
    }

    public void cadastrar(Connection conn, CategoriaDocumento cd){
        Map<String, String> erros = validarCategoriaDocumento(cd);
        String nomeDuplicado = validarNomeDuplicado(conn, cd.getNome(), null);
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().cadastrar(conn, cd);
    }

    public void alterar(Connection conn, CategoriaDocumento cd){
        if(cd.getId() == null || cd.getId() <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        Map<String, String> erros = validarCategoriaDocumento(cd);
        String nomeDuplicado = validarNomeDuplicado(conn, cd.getNome(), cd.getId());
        if(nomeDuplicado != null)
            erros.put("nome", nomeDuplicado);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, cd);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        getDao().excluir(conn, id);
    }

    public CategoriaDocumento buscarPorId(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return getDao().buscarPorId(conn, id);
    }

    public List<CategoriaDocumento> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<CategoriaDocumento> filtrar(Connection conn, String nome){
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
