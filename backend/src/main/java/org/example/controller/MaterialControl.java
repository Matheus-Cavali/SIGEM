package org.example.controller;

import com.google.gson.Gson;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.facade.MaterialFacade;
import org.example.model.Material;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MaterialControl {
    private static final MaterialControl instancia = new MaterialControl();
    private final MaterialFacade facade = MaterialFacade.getInstancia();
    private final Gson gson = new Gson();

    private MaterialControl() {}

    public static MaterialControl getInstancia(){
        return instancia;
    }

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        String email = emailDoToken(auth);
        if (email != null) {
            UsuarioDao uDao = new UsuarioDao();
            Usuario u = uDao.buscarPorEmail(email);
            if (u != null) {
                if (u.getNivelAcesso() == 1) return true;
                RecursoSistemaDao rDao = new RecursoSistemaDao();
                for (RecursoSistema r : rDao.listarPorUsuario(u.getId())) {
                    if (r.getNome().equals(recursoNome)) return true;
                }
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
            facade.cadastrar(m);
            return new Resposta(201, "{\"mensagem\":\"Material cadastrado com sucesso\"}");
        }
        catch (Exception e){
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

            List<Material> lista = facade.filtrar(nome, categoriaId);
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
            facade.alterar(m);
            return new Resposta(200, "{\"mensagem\":\"Material atualizado com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao atualizar material\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            facade.excluir(id);
            return new Resposta(200, "{\"mensagem\":\"Material excluído com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir material\"}");
        }
    }
}
