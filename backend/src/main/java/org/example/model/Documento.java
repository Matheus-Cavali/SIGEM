package org.example.model;

import java.time.LocalDate;

public class Documento {
    private int id;
    private String titulo;
    private String caminhoArquivo;
    private LocalDate dataDocumento;
    private Integer categoriaDocumentoId;
    private String categoriaDocumentoNome;
    private Integer usuarioTitularId;
    private String usuarioTitularNome;
    private Integer colaboradorLancouId;
    private String colaboradorLancouNome;

    public Documento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
