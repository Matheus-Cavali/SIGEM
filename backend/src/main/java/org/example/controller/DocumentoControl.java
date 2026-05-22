package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.Documento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.util.Data;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DocumentoControl {
    private static Documento documento;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, type, ctx) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, ctx) ->
                    parseData(json.getAsString()))
            .create();

    public static synchronized Documento getDocumento(){
        if(documento == null)
            documento = new Documento();

        return documento;
    }

    public DocumentoControl() {}

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
            String email = emailDoToken(auth);
            UsuarioDao uDao = new UsuarioDao();
            Connection conn = Conexao.getConexao();
            Usuario colaborador = uDao.buscarPorEmail(conn, email);
            if (colaborador == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");

            Documento d = montarDocumento(json, colaborador.getId());
            int id = getDocumento().cadastrar(conn, d);
            return new Resposta(201, "{\"mensagem\":\"Documento cadastrado com sucesso\",\"id\":" + id + "}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao cadastrar documento\"}");
        }
    }

    public Resposta listar(String query){
        try{
            String titulo = null;
            Integer categoriaDocumentoId = null;

            if(query != null){
                for(String param : query.split("&")){
                    String[] pair = param.split("=", 2);
                    if(pair.length == 2){
                        String chave = pair[0];
                        String valor = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        if("titulo".equalsIgnoreCase(chave)){
                            titulo = valor;
                        }
                        else if("categoriaDocumentoId".equalsIgnoreCase(chave) && !valor.isEmpty()){
                            categoriaDocumentoId = Integer.parseInt(valor);
                        }
                    }
                }
            }

            List<Documento> lista = getDocumento().filtrar(Conexao.getConexao(), titulo, categoriaDocumentoId);
            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao listar documentos\"}");
        }
    }

    public Resposta buscarPorId(int id){
        try{
            Documento d = getDocumento().buscarPorId(Conexao.getConexao(), id);
            if (d == null) return new Resposta(404, "{\"erro\":\"Documento não encontrado\"}");
            return new Resposta(200, gson.toJson(d));
        }
        catch (Exception e){
            return new Resposta(500, "{\"erro\":\"Erro ao buscar documento\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            String email = emailDoToken(auth);
            UsuarioDao uDao = new UsuarioDao();
            Connection conn = Conexao.getConexao();
            Usuario colaborador = uDao.buscarPorEmail(conn, email);
            if (colaborador == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");

            Documento existente = getDocumento().buscarPorId(conn, id);
            if (existente == null) return new Resposta(404, "{\"erro\":\"Documento não encontrado\"}");

            Documento d = montarDocumento(json, colaborador.getId());
            d.setId(id);
            getDocumento().alterar(conn, d);
            return new Resposta(200, "{\"mensagem\":\"Documento atualizado com sucesso\"}");
        }
        catch (Exception e){
            String msg = e.getMessage();
            if(msg != null && msg.contains("\"erros\"")){
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"Erro ao atualizar documento\"}");
        }
    }

    public Resposta excluir(String auth, int id){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try{
            getDocumento().excluir(Conexao.getConexao(), id);
            return new Resposta(200, "{\"mensagem\":\"Documento excluído com sucesso\"}");
        }
        catch (Exception e){
            return new Resposta(400, "{\"erro\":\"Erro ao excluir documento\"}");
        }
    }

    public Resposta autorizarUpload(String auth){
        if (!usuarioTemPermissao(auth, "GESTAO_DOACOES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        return new Resposta(200, "{\"mensagem\":\"Upload autorizado\"}");
    }

    private Documento montarDocumento(String json, int colaboradorId){
        JsonObject body = gson.fromJson(json, JsonObject.class);
        Documento d = new Documento();

        if (body.has("titulo") && !body.get("titulo").isJsonNull())
            d.setTitulo(body.get("titulo").getAsString().trim());
        if (body.has("caminhoArquivo") && !body.get("caminhoArquivo").isJsonNull())
            d.setCaminhoArquivo(body.get("caminhoArquivo").getAsString().trim());
        if (body.has("dataDocumento") && !body.get("dataDocumento").isJsonNull())
            d.setDataDocumento(parseData(body.get("dataDocumento").getAsString()));
        if (body.has("categoriaDocumentoId") && !body.get("categoriaDocumentoId").isJsonNull())
            d.setCategoriaDocumentoId(body.get("categoriaDocumentoId").getAsInt());

        d.setUsuarioTitularId(colaboradorId);
        d.setColaboradorLancouId(colaboradorId);

        return d;
    }

    private static LocalDate parseData(String dataTexto){
        if (dataTexto == null || dataTexto.trim().isEmpty()) return null;
        try{
            return LocalDate.parse(dataTexto, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        catch (DateTimeParseException e){
            return Data.parseFlexivel(dataTexto);
        }
    }
}
