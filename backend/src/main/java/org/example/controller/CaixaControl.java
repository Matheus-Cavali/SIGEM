package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.model.Caixa;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaixaControl {
    private static Caixa caixa;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
            .create();

    public static synchronized Caixa getCaixa(){
        if(caixa == null)
            caixa = new Caixa();
        return caixa;
    }

    public CaixaControl() {}

    private String emailDoToken(String auth){
        if(auth != null && auth.startsWith("Bearer ")){
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome){
        String email = emailDoToken(auth);
        if(email != null){
            try{
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if(u != null){
                    if(u.getNivelAcesso() == 1)
                        return true;
                    for(RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())){
                        if(r.getNome().equals(recursoNome))
                            return true;
                    }
                }
            }
            catch(Exception e){
                return false;
            }
        }
        return false;
    }

    public Resposta abrir(String auth, String json){
        if(!usuarioTemPermissao(auth, "GESTAO_CAIXA")){
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            Caixa c = new Caixa();
            c.setColaboradorAbriuId(u.getId());

            getCaixa().abrir(conn, c);
            return new Resposta(201, gson.toJson(c));
        }
        catch(Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Falha ao abrir caixa\"}");
        }
    }

    public Resposta fechar(String auth, int id, String json){
        if(!usuarioTemPermissao(auth, "GESTAO_CAIXA")){
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);

            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            Caixa c = new Caixa();
            c.setId(id);
            c.setValorFechamento(jsonObj.get("valorFechamento").getAsBigDecimal());
            c.setColaboradorFechouId(u.getId());

            getCaixa().fechar(conn, c);

            Caixa atualizado = getCaixa().buscarPorId(conn, id);
            return new Resposta(200, gson.toJson(atualizado));
        }
        catch(Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta movimentar(String auth, int id, String json){
        if(!usuarioTemPermissao(auth, "GESTAO_CAIXA")){
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);
            BigDecimal valor = jsonObj.get("valor").getAsBigDecimal();

            getCaixa().movimentar(Conexao.getConexao(), id, valor);

            Caixa atualizado = getCaixa().buscarPorId(Conexao.getConexao(), id);
            return new Resposta(200, gson.toJson(atualizado));
        }
        catch(Exception e){
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta buscarAberto(){
        try{
            Caixa c = getCaixa().buscarCaixaAberto(Conexao.getConexao());
            if(c == null)
                return new Resposta(200, "null");
            return new Resposta(200, gson.toJson(c));
        }
        catch(Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao buscar caixa aberto\"}");
        }
    }

    public Resposta listar(){
        try{
            List<Caixa> lista = getCaixa().listarTodos(Conexao.getConexao());
            return new Resposta(200, gson.toJson(lista));
        }
        catch(Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar caixas\"}");
        }
    }
}
