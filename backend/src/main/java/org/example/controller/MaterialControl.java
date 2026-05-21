package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.Material;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

public class MaterialControl {
    private static Material material;
    private final Gson gson = new Gson();

    public static synchronized Material getMaterial(){
        if(material == null)
            material = new Material();

        return material;
    }

    public MaterialControl() {}

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
            Material m = gson.fromJson(json, Material.class);
            getMaterial().cadastrar(Conexao.getConexao(), m);
            return new Resposta(201, "{\"mensagem\":\"Material cadastrado com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar material\"}");
        }
    }

    public Resposta listar(String query){
        try{
            String nome = null;
            Integer categoriaId = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2){
                        if("nome".equalsIgnoreCase(pair[0])){
                            nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        }
                        else if("categoriaId".equalsIgnoreCase(pair[0])){
                            categoriaId = Integer.parseInt(pair[1]);
                        }
                    }
                }
            }

            List<Material> lista = getMaterial().filtrar(Conexao.getConexao(), nome, categoriaId);
            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar materiais\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            Material m = gson.fromJson(json, Material.class);
            m.setId(id);
            getMaterial().alterar(Conexao.getConexao(), m);
            return new Resposta(200, "{\"mensagem\":\"Material atualizado com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao atualizar material\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            getMaterial().excluir(Conexao.getConexao(), id);
            return new Resposta(200, "{\"mensagem\":\"Material excluído com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir material\"}");
        }
    }
}
