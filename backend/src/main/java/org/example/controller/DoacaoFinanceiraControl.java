package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.model.*;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.util.Data;

public class DoacaoFinanceiraControl {
    private static DoacaoFinanceira doacaoFinanceira;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

    public static synchronized DoacaoFinanceira getDoacaoFinanceira(){
        if(doacaoFinanceira == null)
            doacaoFinanceira = new DoacaoFinanceira();
        return doacaoFinanceira;
    }

    private String emailDoToken(String auth) {
        if(auth != null && auth.startsWith("Bearer "))
            return org.example.util.Token.validarToken(auth.substring(7));
        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome){
        String email = emailDoToken(auth);
        if(email != null){
            try{
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if(u != null){
                    if(u.getNivelAcesso() == 1) return true;
                    for(RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())){
                        if(r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch(Exception e){ return false; }
        }
        return false;
    }

    public Resposta cadastrar(String auth, String json){
        if(!usuarioTemPermissao(auth, "GESTAO_DOACOES"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);

            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            DoacaoFinanceira df = new DoacaoFinanceira();
            df.setValor(jsonObj.get("valor").getAsBigDecimal());
            df.setCategoriaFinanceiraId(jsonObj.get("categoriaFinanceiraId").getAsInt());
            df.setData(LocalDate.now());
            df.setColaboradorId(u.getId());

            getDoacaoFinanceira().cadastrar(conn, df);
            return new Resposta(201, "{\"mensagem\":\"Doação financeira cadastrada com sucesso\"}");
        } catch(Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\""))
                return new Resposta(400, msg);
            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar doação financeira\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if(!usuarioTemPermissao(auth, "GESTAO_DOACOES"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);

            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            DoacaoFinanceira df = new DoacaoFinanceira();
            df.setId(id);
            df.setValor(jsonObj.get("valor").getAsBigDecimal());
            df.setCategoriaFinanceiraId(jsonObj.get("categoriaFinanceiraId").getAsInt());
            df.setData(LocalDate.now());
            df.setColaboradorId(u.getId());

            getDoacaoFinanceira().alterar(conn, df);
            return new Resposta(200, "{\"mensagem\":\"Doação financeira atualizada com sucesso\"}");
        } catch(Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\""))
                return new Resposta(400, msg);
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if(!usuarioTemPermissao(auth, "GESTAO_DOACOES"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try{
            getDoacaoFinanceira().excluir(Conexao.getConexao(), id);
            return new Resposta(200, "{\"mensagem\":\"Doação financeira excluída com sucesso\"}");
        } catch(Exception e){
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta listar(String query){
        try{
            Integer categoriaId = null;
            LocalDate dataInicio = null;
            LocalDate dataFim = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2){
                        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                        if("categoriaId".equalsIgnoreCase(key))
                            categoriaId = Integer.parseInt(value);
                        else if("dataInicio".equalsIgnoreCase(key))
                            dataInicio = Data.parseFlexivel(value);
                        else if("dataFim".equalsIgnoreCase(key))
                            dataFim = Data.parseFlexivel(value);
                    }
                }
            }

            List<DoacaoFinanceira> lista = getDoacaoFinanceira().filtrar(Conexao.getConexao(), categoriaId, dataInicio, dataFim);
            return new Resposta(200, gson.toJson(lista));
        } catch(Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar doações financeiras\"}");
        }
    }
}
