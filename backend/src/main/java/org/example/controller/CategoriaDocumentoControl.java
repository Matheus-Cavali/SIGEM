package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.CategoriaDocumento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

public class CategoriaDocumentoControl {
    private static CategoriaDocumento categoriaDocumento;
    private final Gson gson = new Gson();

    public static synchronized CategoriaDocumento getCategoriaDocumento(){
        if(categoriaDocumento == null)
            categoriaDocumento = new CategoriaDocumento();

        return categoriaDocumento;
    }

    public CategoriaDocumentoControl() {}

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome){
        String email = emailDoToken(auth);
        if (email != null) {
            try{
                UsuarioDao uDao = new UsuarioDao();
                Connection conn = Conexao.getConexao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            }
            catch (Exception e){
                return false;
            }
        }
        return false;
    }

    public Resposta cadastrar(String auth, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            CategoriaDocumento cd = gson.fromJson(json, CategoriaDocumento.class);
            getCategoriaDocumento().cadastrar(Conexao.getConexao(), cd);
            return new Resposta(201, "{\"mensagem\":\"Categoria de documento cadastrada com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar categoria de documento\"}");
        }
    }

    public Resposta listar(String query){
        try{
            String nome = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2 && "nome".equalsIgnoreCase(pair[0])){
                        nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }

            List<CategoriaDocumento> lista = getCategoriaDocumento().filtrar(Conexao.getConexao(), nome);
            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias de documento\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            CategoriaDocumento cd = gson.fromJson(json, CategoriaDocumento.class);
            cd.setId(id);
            getCategoriaDocumento().alterar(Conexao.getConexao(), cd);
            return new Resposta(200, "{\"mensagem\":\"Categoria de documento atualizada com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao atualizar categoria de documento\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            getCategoriaDocumento().excluir(Conexao.getConexao(), id);
            return new Resposta(200, "{\"mensagem\":\"Categoria de documento excluída com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir categoria de documento\"}");
        }
    }
}
