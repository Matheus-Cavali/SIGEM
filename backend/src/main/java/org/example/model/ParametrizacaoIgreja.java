package org.example.model;

import org.example.dao.ParametrizacaoIgrejaDao;

import java.sql.Connection;
import java.sql.SQLException;

public class ParametrizacaoIgreja {
    private Integer id;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String caminhoLogo;
    private String corPrimaria;
    private String corSecundaria;
    private String telefone;
    private String email;
    private String site;
    private String endereco;

    public static ParametrizacaoIgreja buscar(Connection conn) throws SQLException {
        ParametrizacaoIgrejaDao dao = new ParametrizacaoIgrejaDao();
        ParametrizacaoIgreja parametros = dao.buscar(conn);
        if (parametros == null) {
            return parametrosPadrao();
        }
        return parametros;
    }

    public static ParametrizacaoIgreja salvar(Connection conn, ParametrizacaoIgreja parametros) throws SQLException {
        validar(parametros);
        aplicarDefaults(parametros);
        ParametrizacaoIgrejaDao dao = new ParametrizacaoIgrejaDao();
        return dao.salvar(conn, parametros);
    }

    public static ParametrizacaoIgreja salvarLogo(Connection conn, String caminhoLogo) throws SQLException {
        ParametrizacaoIgreja parametros = buscar(conn);
        parametros.setCaminhoLogo(caminhoLogo);
        return salvar(conn, parametros);
    }

    private static void validar(ParametrizacaoIgreja parametros) {
        if (parametros == null) {
            throw new IllegalArgumentException("Parâmetros da igreja são obrigatórios.");
        }
        if (parametros.getNomeFantasia() == null || parametros.getNomeFantasia().isBlank()) {
            throw new IllegalArgumentException("Nome fantasia é obrigatório.");
        }
    }

    private static void aplicarDefaults(ParametrizacaoIgreja parametros) {
        if (parametros.getCorPrimaria() == null || parametros.getCorPrimaria().isBlank()) {
            parametros.setCorPrimaria("#1f4f82");
        }
        if (parametros.getCorSecundaria() == null || parametros.getCorSecundaria().isBlank()) {
            parametros.setCorSecundaria("#7d0a1e");
        }
    }

    private static ParametrizacaoIgreja parametrosPadrao() {
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

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}
