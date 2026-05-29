package org.example.model;

import com.google.gson.Gson;
import org.example.dao.ParametrizacaoIgrejaDao;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParametrizacaoIgreja {
    private Integer id;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String caminhoLogo;
    private String caminhoLogoGrande;
    private String corPrimaria;
    private String corSecundaria;
    private String telefone;
    private String email;
    private String site;
    private Integer enderecoId;
    private Endereco endereco;

    private static ParametrizacaoIgrejaDao dao;

    public static synchronized ParametrizacaoIgrejaDao getDao(){
        if(dao == null)
            dao = new ParametrizacaoIgrejaDao();

        return dao;
    }

    public ParametrizacaoIgreja() {}

    public static Map<String, String> validarParametrizacao(ParametrizacaoIgreja p){
        Map<String, String> erros = new LinkedHashMap<>();
        if(p.getNomeFantasia() == null || p.getNomeFantasia().trim().isEmpty())
            erros.put("nomeFantasia", "Nome fantasia é obrigatório");
        return erros;
    }

    public void cadastrar(Connection conn, ParametrizacaoIgreja p){
        aplicarDefaults(p);
        Map<String, String> erros = validarParametrizacao(p);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().cadastrar(conn, p);
    }

    public void alterar(Connection conn, ParametrizacaoIgreja p){
        aplicarDefaults(p);
        Map<String, String> erros = validarParametrizacao(p);
        if(!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, p);
    }

    public void salvar(Connection conn, ParametrizacaoIgreja p){
        if (p.getEndereco() != null) {
            Endereco e = new Endereco();
            e.setId(p.getEndereco().getId());
            e.setCep(p.getEndereco().getCep());
            e.setLogradouro(p.getEndereco().getLogradouro());
            e.setNumero(p.getEndereco().getNumero());
            e.setComplemento(p.getEndereco().getComplemento());
            e.setBairro(p.getEndereco().getBairro());
            e.setCidade(p.getEndereco().getCidade());
            e.setUf(p.getEndereco().getUf());
            e.salvar(conn, e);
            p.setEnderecoId(e.getId());
            p.setEndereco(null);
        }

        ParametrizacaoIgreja atual = buscar(conn);
        if(atual == null || atual.getId() == null)
            cadastrar(conn, p);
        else{
            p.setId(atual.getId());
            alterar(conn, p);
        }
    }

    public void salvarLogo(Connection conn, String caminhoLogo){
        ParametrizacaoIgreja atual = buscar(conn);
        if(atual == null)
            atual = parametrosPadrao();
        atual.setCaminhoLogo(caminhoLogo);
        salvar(conn, atual);
    }

    public void salvarLogoGrande(Connection conn, String caminhoLogoGrande){
        ParametrizacaoIgreja atual = buscar(conn);
        if(atual == null)
            atual = parametrosPadrao();
        atual.setCaminhoLogoGrande(caminhoLogoGrande);
        salvar(conn, atual);
    }

    public void excluir(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        getDao().excluir(conn, id);
    }

    public ParametrizacaoIgreja buscarPorId(Connection conn, Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return getDao().buscarPorId(conn, id);
    }

    public ParametrizacaoIgreja buscar(Connection conn){
        ParametrizacaoIgreja parametros = getDao().buscar(conn);
        if(parametros == null)
            return parametrosPadrao();

        if (parametros.getEnderecoId() != null)
            parametros.setEndereco(new Endereco().buscarPorId(conn, parametros.getEnderecoId()));

        return parametros;
    }

    public List<ParametrizacaoIgreja> listarTodos(Connection conn){
        return getDao().listarTodos(conn);
    }

    public List<ParametrizacaoIgreja> filtrar(Connection conn, String nomeFantasia){
        return getDao().listar(conn, nomeFantasia);
    }

    private void aplicarDefaults(ParametrizacaoIgreja p){
        if(p.getCorPrimaria() == null || p.getCorPrimaria().trim().isEmpty())
            p.setCorPrimaria("#1f4f82");
        if(p.getCorSecundaria() == null || p.getCorSecundaria().trim().isEmpty())
            p.setCorSecundaria("#7d0a1e");
    }

    private ParametrizacaoIgreja parametrosPadrao(){
        ParametrizacaoIgreja parametros = new ParametrizacaoIgreja();
        parametros.setNomeFantasia("Igreja");
        parametros.setCorPrimaria("#1f4f82");
        parametros.setCorSecundaria("#7d0a1e");
        return parametros;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getCaminhoLogo() { return caminhoLogo; }
    public void setCaminhoLogo(String caminhoLogo) { this.caminhoLogo = caminhoLogo; }

    public String getCaminhoLogoGrande() { return caminhoLogoGrande; }
    public void setCaminhoLogoGrande(String caminhoLogoGrande) { this.caminhoLogoGrande = caminhoLogoGrande; }

    public String getCorPrimaria() { return corPrimaria; }
    public void setCorPrimaria(String corPrimaria) { this.corPrimaria = corPrimaria; }

    public String getCorSecundaria() { return corSecundaria; }
    public void setCorSecundaria(String corSecundaria) { this.corSecundaria = corSecundaria; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public Integer getEnderecoId() { return enderecoId; }
    public void setEnderecoId(Integer enderecoId) { this.enderecoId = enderecoId; }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
}
