package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.dao.AporteInvestimentoDao;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.AporteInvestimento;
import org.example.model.InvestimentoFuturo;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.util.Data;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CInvestimento {

    private static CInvestimento instancia;
    private CInvestimento() {}
    public static CInvestimento getInstancia() {
        if (instancia == null) instancia = new CInvestimento();
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

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            Usuario u = new UsuarioDao().buscarPorEmail(email);
            if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO");
        }
        return false;
    }

    public Resposta registrarInvestimento(String auth, String json) {
        if (!usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        String nome = body.get("nome").getAsString();
        BigDecimal meta = body.get("valorMeta").getAsBigDecimal();
        String dataStr = body.get("dataAbertura").getAsString();
        int colaboradorId = body.get("colaboradorId").getAsInt();

        InvestimentoFuturo inv = new InvestimentoFuturo();
        inv.setNome(nome); inv.setValorMeta(meta);
        inv.setDataAbertura(Data.parseFlexivel(dataStr)); inv.setColaboradorId(colaboradorId);

        String erro = inv.validar(dataStr);
        if (erro != null) return new Resposta(400, "{\"erro\":\"" + erro + "\"}");

        int id = inv.salvar();
        if (id > 0) return new Resposta(201, "{\"mensagem\":\"Investimento registrado com sucesso\",\"id\":" + id + "}");
        return new Resposta(500, "{\"erro\":\"Erro ao registrar investimento\"}");
    }

    public Resposta listarInvestimentos(String query) {
        String nome = null, status = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if ("status".equalsIgnoreCase(kv[0])) status = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        List<InvestimentoFuturo> lista = new InvestimentoFuturoDao().listar(nome, status);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            InvestimentoFuturo inv = lista.get(i);
            json.append("{\"id\":").append(inv.getId()).append(",");
            json.append("\"nome\":\"").append(escaparJson(inv.getNome())).append("\",");
            json.append("\"valorMeta\":").append(inv.getValorMeta()).append(",");
            json.append("\"dataAbertura\":\"").append(inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\",");
            json.append("\"status\":\"").append(inv.getStatus()).append("\",");
            json.append("\"saldoAtual\":").append(inv.getSaldoAtual());
            json.append("}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        return new Resposta(200, json.toString());
    }

    public Resposta buscarInvestimento(int id) {
        InvestimentoFuturo inv = new InvestimentoFuturoDao().buscarPorId(id);
        if (inv == null) return new Resposta(404, "{\"erro\":\"Investimento n\u00e3o encontrado\"}");
        JsonObject resp = new JsonObject();
        resp.addProperty("id", inv.getId()); resp.addProperty("nome", inv.getNome());
        resp.addProperty("valorMeta", inv.getValorMeta());
        resp.addProperty("dataAbertura", inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        resp.addProperty("status", inv.getStatus()); resp.addProperty("saldoAtual", inv.getSaldoAtual());
        return new Resposta(200, resp.toString());
    }

    public Resposta atualizarInvestimento(String auth, int id, String json) {
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        boolean alterandoStatus = body.has("status");
        if (alterandoStatus && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        InvestimentoFuturo inv = dao.buscarPorId(id);
        if (inv == null) return new Resposta(404, "{\"erro\":\"Investimento n\u00e3o encontrado\"}");
        if (body.has("nome")) inv.setNome(body.get("nome").getAsString());
        if (body.has("valorMeta")) inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
        if (body.has("status")) inv.setStatus(body.get("status").getAsString());
        if (dao.atualizar(inv)) return new Resposta(200, "{\"mensagem\":\"Investimento atualizado\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar\"}");
    }

    public Resposta lancarAporte(String auth, int investimentoId, String json) {
        if (!usuarioTemPermissao(auth, "LANCAR_APORTE")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        BigDecimal valor = body.get("valorAporte").getAsBigDecimal();
        String dataStr = body.get("dataAporte").getAsString();
        int colaboradorId = body.get("colaboradorId").getAsInt();

        AporteInvestimento aporte = new AporteInvestimento();
        aporte.setInvestimentoFuturoId(investimentoId); aporte.setValorAporte(valor);
        aporte.setDataAporte(Data.parseFlexivel(dataStr)); aporte.setColaboradorId(colaboradorId);

        String erro = aporte.validar(dataStr);
        if (erro != null) return new Resposta(400, "{\"erro\":\"" + erro + "\"}");

        InvestimentoFuturoDao invDao = new InvestimentoFuturoDao();
        if (invDao.buscarPorId(investimentoId) == null) return new Resposta(404, "{\"erro\":\"Investimento n\u00e3o encontrado\"}");

        int id = new AporteInvestimentoDao().inserir(aporte);
        if (id > 0) {
            BigDecimal saldo = invDao.calcularSaldo(investimentoId);
            return new Resposta(201, "{\"mensagem\":\"Aporte registrado com sucesso\",\"id\":" + id + ",\"saldoAtual\":" + saldo + "}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao registrar aporte\"}");
    }

    public Resposta listarAportes(int investimentoId) {
        List<AporteInvestimento> lista = new AporteInvestimentoDao().listarPorInvestimento(investimentoId);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            AporteInvestimento a = lista.get(i);
            json.append("{\"id\":").append(a.getId()).append(",");
            json.append("\"valorAporte\":").append(a.getValorAporte()).append(",");
            json.append("\"dataAporte\":\"").append(a.getDataAporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\"}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        return new Resposta(200, json.toString());
    }

    public Resposta removerInvestimento(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        if (new InvestimentoFuturoDao().deletar(id)) return new Resposta(200, "{\"mensagem\":\"Investimento removido\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao remover investimento\"}");
    }

    public Resposta removerAporte(String auth, int aporteId) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        if (new AporteInvestimentoDao().deletar(aporteId)) return new Resposta(200, "{\"mensagem\":\"Aporte removido\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao remover aporte\"}");
    }

    public Resposta atualizarAporte(String auth, int aporteId, String json) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        AporteInvestimento aporte = new AporteInvestimento();
        aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());
        aporte.setDataAporte(Data.parseFlexivel(body.get("dataAporte").getAsString()));
        if (new AporteInvestimentoDao().atualizar(aporteId, aporte)) return new Resposta(200, "{\"mensagem\":\"Aporte atualizado\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar aporte\"}");
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
