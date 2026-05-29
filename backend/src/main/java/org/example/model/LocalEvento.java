package org.example.model;

import com.google.gson.Gson;
import org.example.dao.LocalEventoDao;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalEvento {

    private Integer id;
    private String nome;
    private String endereco;

    private static LocalEventoDao dao;

    public static synchronized LocalEventoDao getDao(){
        if(dao == null)
            dao = new LocalEventoDao();

        return dao;
    }

    public LocalEvento(){}

    public LocalEvento(Integer id, String nome, String endereco){
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
    }

    public LocalEvento(String nome, String endereco){
        this(null, nome, endereco);
    }

    public static Map<String, String> validar(LocalEvento le){
        Map<String, String> erros = new LinkedHashMap<>();

        if(le.getNome() == null || le.getNome().trim().isEmpty())
            erros.put("nome", "Nome é obrigatório");

        return erros;
    }

    public static String validarDuplicado(Connection conn, String nome, Integer idAtual){
        LocalEvento existente = getDao().buscarPorNome(conn, nome);

        if(existente != null && (idAtual == null || !existente.getId().equals(idAtual))){
            return "Local já cadastrado.";
        }

        return null;
    }

    public void cadastrar(Connection conn, LocalEvento le){

        Map<String, String> erros = validar(le);

        String dup = validarDuplicado(conn, le.getNome(), null);

        if(dup != null)
            erros.put("nome", dup);

        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        getDao().cadastrar(conn, le);
    }

    public void alterar(Connection conn, LocalEvento le){

        if(le.getId() == null || le.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");

        Map<String, String> erros = validar(le);

        String dup = validarDuplicado(conn, le.getNome(), le.getId());

        if(dup != null)
            erros.put("nome", dup);

        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));

        getDao().atualizar(conn, le);
    }

    public void excluir(Connection conn, Integer id){

        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        getDao().excluir(conn, id);
    }

    public LocalEvento buscarPorId(Connection conn, Integer id){

        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        return getDao().buscarPorId(conn, id);
    }

    public List<LocalEvento> listar(Connection conn, String nome){
        return getDao().listar(conn, nome);
    }

    public Integer getId(){ return id; }
    public void setId(Integer id){ this.id = id; }

    public String getNome(){ return nome; }
    public void setNome(String nome){ this.nome = nome; }

    public String getEndereco(){ return endereco; }
    public void setEndereco(String endereco){ this.endereco = endereco; }
}