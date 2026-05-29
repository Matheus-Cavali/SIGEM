package org.example.controller;

import com.google.gson.Gson;
import org.example.conexao.Conexao;

import org.example.model.ParametrizacaoIgreja;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.sql.Connection;

public class ParametrizacaoIgrejaControl {
    private static ParametrizacaoIgreja parametrizacaoIgreja;
    private final Gson gson = new Gson();

    public static synchronized ParametrizacaoIgreja getParametrizacaoIgreja(){
        if(parametrizacaoIgreja == null)
            parametrizacaoIgreja = new ParametrizacaoIgreja();

        return parametrizacaoIgreja;
    }

    public ParametrizacaoIgrejaControl() {}

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
                    if (u.getNivelAcesso() == 1) return true;
                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
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

    public Resposta buscar(){
        try{
            return new Resposta(200, gson.toJson(getParametrizacaoIgreja().buscar(Conexao.getConexao())));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao buscar configurações da igreja\"}");
        }
    }

    public Resposta salvar(String auth, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            ParametrizacaoIgreja parametros = gson.fromJson(json, ParametrizacaoIgreja.class);
            Connection conn = Conexao.getConexao();
            getParametrizacaoIgreja().salvar(conn, parametros);
            return new Resposta(200, gson.toJson(getParametrizacaoIgreja().buscar(conn)));
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao salvar configurações da igreja\"}");
        }
    }

    public Resposta salvarLogo(String auth, String caminhoLogo){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            Connection conn = Conexao.getConexao();
            getParametrizacaoIgreja().salvarLogo(conn, caminhoLogo);
            return new Resposta(200, gson.toJson(getParametrizacaoIgreja().buscar(conn)));
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao salvar logo da igreja\"}");
        }
    }
}
