package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.model.DoacaoMaterial;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.util.Data;

public class DoacaoMaterialControl {
    private static DoacaoMaterial doacaoMaterial;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

    public static synchronized DoacaoMaterial getDoacaoMaterial(){
        if(doacaoMaterial == null)
            doacaoMaterial = new DoacaoMaterial();
        return doacaoMaterial;
    }

    public DoacaoMaterialControl() {}

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
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
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
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);

            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            DoacaoMaterial dm = new DoacaoMaterial();
            dm.setMaterialId(jsonObj.get("materialId").getAsInt());
            dm.setQuantidade(jsonObj.get("quantidade").getAsInt());
            dm.setData(LocalDate.now());
            dm.setColaboradorId(u.getId());

            getDoacaoMaterial().cadastrar(conn, dm);
            return new Resposta(201, "{\"mensagem\":\"Doação de material cadastrada com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Falha ao cadastrar doação de material\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            JsonObject jsonObj = gson.fromJson(json, JsonObject.class);

            String email = emailDoToken(auth);
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorEmail(conn, email);
            if(u == null)
                return new Resposta(400, "{\"erro\":\"Usuário não encontrado\"}");

            DoacaoMaterial dm = new DoacaoMaterial();
            dm.setId(id);
            dm.setMaterialId(jsonObj.get("materialId").getAsInt());
            dm.setQuantidade(jsonObj.get("quantidade").getAsInt());
            if(jsonObj.has("data") && !jsonObj.get("data").getAsString().isEmpty())
                dm.setData(Data.parseFlexivel(jsonObj.get("data").getAsString()));
            dm.setColaboradorId(u.getId());

            getDoacaoMaterial().alterar(conn, dm);
            return new Resposta(200, "{\"mensagem\":\"Doação de material atualizada com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            getDoacaoMaterial().excluir(Conexao.getConexao(), id);
            return new Resposta(200, "{\"mensagem\":\"Doação de material excluída com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta listar(String query){
        try{
            String materialNome = null;
            Integer categoriaId = null;
            LocalDate dataInicio = null;
            LocalDate dataFim = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=");
                    if(pair.length == 2){
                        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                        if("materialNome".equalsIgnoreCase(key)){
                            materialNome = value;
                        }
                        else if("categoriaId".equalsIgnoreCase(key)){
                            categoriaId = Integer.parseInt(value);
                        }
                        else if("dataInicio".equalsIgnoreCase(key)){
                            dataInicio = Data.parseFlexivel(value);
                        }
                        else if("dataFim".equalsIgnoreCase(key)){
                            dataFim = Data.parseFlexivel(value);
                        }
                    }
                }
            }

            List<DoacaoMaterial> lista = getDoacaoMaterial().filtrar(Conexao.getConexao(), materialNome, categoriaId, dataInicio, dataFim);
            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar doações de material\"}");
        }
    }
}
