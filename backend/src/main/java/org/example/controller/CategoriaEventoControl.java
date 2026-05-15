package org.example.controller;

import com.google.gson.Gson;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.facade.CategoriaEventoFacade;
import org.example.model.CategoriaEvento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CategoriaEventoControl {
    private static final CategoriaEventoControl instancia = new CategoriaEventoControl();
    private final CategoriaEventoFacade facade = CategoriaEventoFacade.getInstancia();
    private final Gson gson = new Gson();

    private CategoriaEventoControl(){}

    public static CategoriaEventoControl getInstancia(){
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
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            CategoriaEvento ce = gson.fromJson(json, CategoriaEvento.class);
            facade.cadastrar(ce);
            return new Resposta(201, "{\"mensagem\":\"Categoria de evento cadastrada com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar categoria de evento\"}");
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

            List<CategoriaEvento> lista =
                    (nome != null && !nome.trim().isEmpty()) ? facade.buscarPorNome(nome) : facade.listarTodos();

            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias de evento\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            CategoriaEvento ce = gson.fromJson(json, CategoriaEvento.class);
            ce.setId(id);
            facade.alterar(ce);
            return new Resposta(200, "{\"mensagem\":\"Categoria de evento atualizada com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao atualizar categoria de evento\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            facade.excluir(id);
            return new Resposta(200, "{\"mensagem\":\"Categoria de evento excluída com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir categoria de evento\"}");
        }
    }
}
