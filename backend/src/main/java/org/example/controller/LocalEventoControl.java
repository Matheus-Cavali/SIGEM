package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;

import org.example.model.LocalEvento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

public class LocalEventoControl {

    private static LocalEvento localEvento;
    private final Gson gson = new Gson();

    public static synchronized LocalEvento getLocalEvento(){
        if(localEvento == null)
            localEvento = new LocalEvento();

        return localEvento;
    }

    public LocalEventoControl(){}

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
                Connection conn = Conexao.getConexao();

                Usuario u = Usuario.buscarPorEmail(conn, email);

                if (u != null) {

                    if (u.getNivelAcesso() == 1)
                        return true;

                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome))
                            return true;
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

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try{
            LocalEvento le = gson.fromJson(json, LocalEvento.class);

            getLocalEvento().cadastrar(Conexao.getConexao(), le);

            return new Resposta(201, "{\"mensagem\":\"Local de evento cadastrado com sucesso\"}");
        }
        catch (Exception e){

            String msg = e.getMessage();

            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }

            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar local de evento\"}");
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

            List<LocalEvento> lista =
                    getLocalEvento().listar(Conexao.getConexao(), nome);

            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar locais de evento\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try{
            LocalEvento le = gson.fromJson(json, LocalEvento.class);

            le.setId(id);

            getLocalEvento().alterar(Conexao.getConexao(), le);

            return new Resposta(200, "{\"mensagem\":\"Local de evento atualizado com sucesso\"}");
        }
        catch (Exception e){

            String msg = e.getMessage();

            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }

            return new Resposta(400, "{\"erro\":\"Erro ao atualizar local de evento\"}");
        }
    }

    public Resposta excluir(String auth, int id){

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try{
            getLocalEvento().excluir(Conexao.getConexao(), id);

            return new Resposta(200, "{\"mensagem\":\"Local de evento excluído com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir local de evento\"}");
        }
    }
}