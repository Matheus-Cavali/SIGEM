package org.example.model;

import com.google.gson.Gson;
import org.example.dao.DocumentoDao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Documento {
    private Integer id;
    private String titulo;
    private String caminhoArquivo;
    private LocalDate dataDocumento;
    private Integer categoriaDocumentoId;
    private String categoriaDocumentoNome;
    private Integer usuarioTitularId;
    private String usuarioTitularNome;
    private Integer colaboradorLancouId;
    private String colaboradorLancouNome;

    private static DocumentoDao dao;

    public static synchronized DocumentoDao getDao(){
        if(dao == null)
            dao = new DocumentoDao();

        return dao;
    }

    public Documento() {}

    public Documento(Integer id, String titulo, String caminhoArquivo, LocalDate dataDocumento, Integer categoriaDocumentoId, Integer usuarioTitularId, Integer colaboradorLancouId) {
        this.id = id;
        this.titulo = titulo;
        this.caminhoArquivo = caminhoArquivo;
        this.dataDocumento = dataDocumento;
        this.categoriaDocumentoId = categoriaDocumentoId;
        this.usuarioTitularId = usuarioTitularId;
        this.colaboradorLancouId = colaboradorLancouId;
    }

    public static Map<String, String> validarDocumento(Documento d){
        Map<String, String> erros = new LinkedHashMap<>();
        if(d.getTitulo() == null || d.getTitulo().trim().isEmpty())
            erros.put("titulo", "Título do documento é obrigatório");
        if(d.getCaminhoArquivo() == null || d.getCaminhoArquivo().trim().isEmpty())
            erros.put("arquivo", "Arquivo do documento é obrigatório");
        if(d.getDataDocumento() == null)
            erros.put("dataDocumento", "Data do documento é obrigatória");
        if(d.getCategoriaDocumentoId() == null || d.getCategoriaDocumentoId() <= 0)
            erros.put("categoriaDocumentoId", "Categoria do documento é obrigatória");
        if(d.getUsuarioTitularId() == null || d.getUsuarioTitularId() <= 0)
            erros.put("usuarioTitularId", "Usuário titular é obrigatório");
        if(d.getColaboradorLancouId() == null || d.getColaboradorLancouId() <= 0)
            erros.put("colaboradorLancouId", "Colaborador responsável é obrigatório");
        return erros;
    }

    public int cadastrar(Connection conn, Documento d){
        Map<String, String> erros = validarDocumento(d);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        return getDao().cadastrar(conn, d);
    }

    public void alterar(Connection conn, Documento d){
        if(d.getId() == null || d.getId() <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        Map<String, String> erros = validarDocumento(d);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, d);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        getDao().excluir(conn, id);
    }

    public Documento buscarPorId(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return getDao().buscarPorId(conn, id);
    }

    public List<Documento> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<Documento> filtrar(Connection conn, String titulo, Integer categoriaDocumentoId){
        return getDao().listar(conn, titulo, categoriaDocumentoId);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCaminhoArquivo() { return caminhoArquivo; }
    public void setCaminhoArquivo(String caminhoArquivo) { this.caminhoArquivo = caminhoArquivo; }

    public LocalDate getDataDocumento() { return dataDocumento; }
    public void setDataDocumento(LocalDate dataDocumento) { this.dataDocumento = dataDocumento; }

    public Integer getCategoriaDocumentoId() { return categoriaDocumentoId; }
    public void setCategoriaDocumentoId(Integer categoriaDocumentoId) { this.categoriaDocumentoId = categoriaDocumentoId; }

    public String getCategoriaDocumentoNome() { return categoriaDocumentoNome; }
    public void setCategoriaDocumentoNome(String categoriaDocumentoNome) { this.categoriaDocumentoNome = categoriaDocumentoNome; }

    public Integer getUsuarioTitularId() { return usuarioTitularId; }
    public void setUsuarioTitularId(Integer usuarioTitularId) { this.usuarioTitularId = usuarioTitularId; }

    public String getUsuarioTitularNome() { return usuarioTitularNome; }
    public void setUsuarioTitularNome(String usuarioTitularNome) { this.usuarioTitularNome = usuarioTitularNome; }

    public Integer getColaboradorLancouId() { return colaboradorLancouId; }
    public void setColaboradorLancouId(Integer colaboradorLancouId) { this.colaboradorLancouId = colaboradorLancouId; }

    public String getColaboradorLancouNome() { return colaboradorLancouNome; }
    public void setColaboradorLancouNome(String colaboradorLancouNome) { this.colaboradorLancouNome = colaboradorLancouNome; }
}
