package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.conexao.Conexao;

import org.example.model.Evento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EventoControl {

    private static Evento evento;
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public static synchronized Evento getEvento() {

        if (evento == null)
            evento = new Evento();

        return evento;
    }

    private static EventoControl instancia;

    private EventoControl() {}

    public static synchronized EventoControl getInstancia() {
        if (instancia == null) instancia = new EventoControl();
        return instancia;
    }

    private String emailDoToken(String auth) {

        if (auth != null && auth.startsWith("Bearer "))
            return org.example.util.Token.validarToken(auth.substring(7));

        return null;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {

        String email = emailDoToken(auth);

        if (email != null) {

            try {

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
            catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    public Resposta cadastrar(String auth, String json) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            Map<String, Object> map = gson.fromJson(json, Map.class);

            Evento evento = new Evento();

            evento.setNome((String) map.get("nome"));
            evento.setDescricao((String) map.get("descricao"));

            String di = (String) map.get("dataInicio");
            String df = (String) map.get("dataFim");

            if (di != null && !di.trim().isEmpty()) {
                String ts = di.replace("T", " ");
                if (!ts.contains(":"))
                    ts += " 00:00:00";
                else if (ts.indexOf(":", ts.indexOf(":") + 1) == -1)
                    ts += ":00";
                try {
                    evento.setDataInicio(Timestamp.valueOf(ts));
                } catch (IllegalArgumentException e) {
                    return new Resposta(400, "{\"erro\":\"Data de início inválida\"}");
                }
            }

            if (df != null && !df.trim().isEmpty()) {
                String ts = df.replace("T", " ");
                if (!ts.contains(":"))
                    ts += " 00:00:00";
                else if (ts.indexOf(":", ts.indexOf(":") + 1) == -1)
                    ts += ":00";
                try {
                    evento.setDataFim(Timestamp.valueOf(ts));
                } catch (IllegalArgumentException e) {
                    return new Resposta(400, "{\"erro\":\"Data de fim inválida\"}");
                }
            }

            Number localId = (Number) map.get("localId");
            if (localId != null)
                evento.setLocalId(localId.intValue());

            Number categoria = (Number) map.get("categoriaEventoId");
            if (categoria != null)
                evento.setCategoriaEventoId(categoria.intValue());

            Number coord = (Number) map.get("coordenadorId");
            if (coord != null)
                evento.setCoordenadorId(coord.intValue());

            evento.setDataRegistro(new java.sql.Date(System.currentTimeMillis()));

            getEvento().cadastrar(Conexao.getConexao(), evento);

            return new Resposta(201,
                    "{\"mensagem\":\"Evento cadastrado com sucesso\"}");
        }
        catch (Exception e) {

            e.printStackTrace();

            String msg = e.getMessage();

            if (msg != null && msg.contains("\"erros\""))
                return new Resposta(400, msg);

            return new Resposta(400,
                    "{\"erro\":\"" + msg + "\"}");
        }
    }

    public Resposta listar(String query) {

        try {

            String nome = null;
            String status = null;

            if (query != null) {

                for (String param : query.split("&")) {

                    String[] pair = param.split("=");

                    if (pair.length == 2) {

                        if ("nome".equalsIgnoreCase(pair[0]))
                            nome = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                        if ("status".equalsIgnoreCase(pair[0]))
                            status = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }

            List<Evento> lista =
                    getEvento().listar(Conexao.getConexao(), nome, status);

            return new Resposta(200, gson.toJson(lista));
        }
        catch (Exception e) {

            return new Resposta(500,
                    "{\"erro\":\"Erro ao listar eventos\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            Map<String, Object> map = gson.fromJson(json, Map.class);

            Evento evento = new Evento();

            evento.setNome((String) map.get("nome"));
            evento.setDescricao((String) map.get("descricao"));

            String di = (String) map.get("dataInicio");
            String df = (String) map.get("dataFim");

            if (di != null && !di.trim().isEmpty()) {
                String ts = di.replace("T", " ");
                if (!ts.contains(":"))
                    ts += " 00:00:00";
                else if (ts.indexOf(":", ts.indexOf(":") + 1) == -1)
                    ts += ":00";
                try {
                    evento.setDataInicio(Timestamp.valueOf(ts));
                } catch (IllegalArgumentException e) {
                    return new Resposta(400, "{\"erro\":\"Data de início inválida\"}");
                }
            }

            if (df != null && !df.trim().isEmpty()) {
                String ts = df.replace("T", " ");
                if (!ts.contains(":"))
                    ts += " 00:00:00";
                else if (ts.indexOf(":", ts.indexOf(":") + 1) == -1)
                    ts += ":00";
                try {
                    evento.setDataFim(Timestamp.valueOf(ts));
                } catch (IllegalArgumentException e) {
                    return new Resposta(400, "{\"erro\":\"Data de fim inválida\"}");
                }
            }

            Number categoria = (Number) map.get("categoriaEventoId");
            if (categoria != null)
                evento.setCategoriaEventoId(categoria.intValue());

            Number local = (Number) map.get("localId");
            if (local != null)
                evento.setLocalId(local.intValue());

            Number coord = (Number) map.get("coordenadorId");
            if (coord != null)
                evento.setCoordenadorId(coord.intValue());

            evento.setId(id);

            getEvento().alterar(Conexao.getConexao(), evento);

            return new Resposta(200,
                    "{\"mensagem\":\"Evento atualizado com sucesso\"}");
        }
        catch (Exception e) {

            String msg = e.getMessage();

            if (msg != null && msg.contains("\"erros\""))
                return new Resposta(400, msg);

            return new Resposta(400,
                    "{\"erro\":\"Erro ao atualizar evento\"}");
        }
    }

    public Resposta abrir(String auth, int id) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            getEvento().abrir(Conexao.getConexao(), id);

            return new Resposta(200,
                    "{\"mensagem\":\"Evento aberto com sucesso\"}");
        }
        catch (Exception e) {

            return new Resposta(400,
                    "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta cancelar(String auth, int id) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            getEvento().cancelar(Conexao.getConexao(), id);

            return new Resposta(200,
                    "{\"mensagem\":\"Evento cancelado com sucesso\"}");
        }
        catch (Exception e) {

            return new Resposta(400,
                    "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta encerrar(String auth,
                             int id,
                             String json) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            Map<?, ?> dados = gson.fromJson(json, Map.class);

            if (dados.get("resultadoFinanceiro") == null)
                return new Resposta(400,
                        "{\"erro\":\"Resultado financeiro é obrigatório\"}");

            BigDecimal resultadoFinanceiro = BigDecimal.valueOf(
                    ((Number) dados.get("resultadoFinanceiro")).doubleValue()
            );

            String observacoesHistorico =
                    (String) dados.get("observacoesHistorico");

            getEvento().encerrar(
                    Conexao.getConexao(),
                    id,
                    resultadoFinanceiro,
                    observacoesHistorico
            );

            return new Resposta(200,
                    "{\"mensagem\":\"Evento encerrado com sucesso\"}");
        }
        catch (Exception e) {

            return new Resposta(400,
                    "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta excluir(String auth, int id) {

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS"))
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {

            getEvento().excluir(Conexao.getConexao(), id);

            return new Resposta(200,
                    "{\"mensagem\":\"Evento excluído com sucesso\"}");
        }
        catch (Exception e) {

            return new Resposta(400,
                    "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }
}