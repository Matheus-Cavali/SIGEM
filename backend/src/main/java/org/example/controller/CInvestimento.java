package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.AporteInvestimentoDao;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.AporteInvestimento;
import org.example.model.InvestimentoFuturo;
import org.example.model.RecursoSistema;
import org.example.model.Usuario;
import org.example.facade.InvestimentoFacade;
import org.example.util.Data;
import org.example.util.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CInvestimento implements HttpHandler {

    private static CInvestimento instancia;
    private CInvestimento() {}
    public static CInvestimento getInstancia() {
        if (instancia == null) instancia = new CInvestimento();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        try {
            if ("POST".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                registrarInvestimento(exchange);
            }
            else if ("GET".equalsIgnoreCase(metodo) && "/api/investimentos".equals(path)) {
                listarInvestimentos(exchange);
            }
            else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                buscarInvestimento(exchange);
            }
            else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                atualizarInvestimento(exchange);
            }
            else if ("GET".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                listarAportes(exchange);
            }
            else if ("POST".equalsIgnoreCase(metodo) && (path.matches("/api/investimentos/\\d+/aportes") || path.matches("/api/investimentos/\\d+/aporte"))) {
                lancarAporte(exchange);
            }
            else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+")) {
                removerInvestimento(exchange);
            }
            else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/investimentos/\\d+/aportes/\\d+")) {
                removerAporte(exchange);
            }
            else {
                enviarResposta(exchange, "{\"erro\":\"Rota não encontrada\"}", 404);
            }
        } catch (Exception e) {
            System.err.println("ERRO no CInvestimento: " + e.getMessage());
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\n", " ") : "Erro desconhecido";
            enviarResposta(exchange, "{\"erro\":\"" + msg + "\"}", 500);
        }
    }

    private String emailDoToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return Token.validarToken(auth.substring(7));
    }

    private boolean usuarioTemPermissao(HttpExchange exchange, String recursoNome) {
        String email = emailDoToken(exchange);
        if (email == null) return false;
        UsuarioDao uDao = new UsuarioDao();
        Usuario u = uDao.buscarPorEmail(email);
        if (u == null) return false;
        if (u.getNivelAcesso() == 1) return true;
        RecursoSistemaDao rDao = new RecursoSistemaDao();
        List<RecursoSistema> permissoes = rDao.listarPorUsuario(u.getId());
        for (RecursoSistema r : permissoes) {
            if (r.getNome().equals(recursoNome)) return true;
        }
        return false;
    }

    private void registrarInvestimento(HttpExchange exchange) throws IOException {
        if (!usuarioTemPermissao(exchange, "REGISTRAR_INVESTIMENTO")) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Você não tem permissão para registrar investimentos.\"}", 403);
            return;
        }
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        String nome = body.get("nome").getAsString();
        BigDecimal meta = body.get("valorMeta").getAsBigDecimal();
        String dataStr = body.get("dataAbertura").getAsString();
        int colaboradorId = body.get("colaboradorId").getAsInt();

        InvestimentoFacade facade = new InvestimentoFacade();
        String erro = facade.validarDadosInvestimento(nome, meta, dataStr);
        if (erro != null) {
            enviarResposta(exchange, "{\"erro\":\"" + erro + "\"}", 400);
            return;
        }

        InvestimentoFuturo inv = new InvestimentoFuturo();
        inv.setNome(nome);
        inv.setValorMeta(meta);
        inv.setDataAbertura(Data.parseFlexivel(dataStr));
        inv.setColaboradorId(colaboradorId);

        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        int id = dao.inserir(inv);

        if (id > 0) {
            enviarResposta(exchange, "{\"mensagem\":\"Investimento registrado com sucesso\",\"id\":" + id + "}", 201);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao registrar investimento\"}", 500);
        }
    }

    private void listarInvestimentos(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String nome = null, status = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if ("status".equalsIgnoreCase(kv[0])) status = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        List<InvestimentoFuturo> lista = dao.listar(nome, status);

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            InvestimentoFuturo inv = lista.get(i);
            json.append("{");
            json.append("\"id\":").append(inv.getId()).append(",");
            json.append("\"nome\":\"").append(escaparJson(inv.getNome())).append("\",");
            json.append("\"valorMeta\":").append(inv.getValorMeta()).append(",");
            json.append("\"dataAbertura\":\"").append(inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\",");
            json.append("\"status\":\"").append(inv.getStatus()).append("\",");
            json.append("\"saldoAtual\":").append(inv.getSaldoAtual());
            json.append("}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        enviarResposta(exchange, json.toString(), 200);
    }

    private void buscarInvestimento(HttpExchange exchange) throws IOException {
        if (emailDoToken(exchange) == null) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Faça login.\"}", 401);
            return;
        }
        int id = extrairId(exchange.getRequestURI().getPath());
        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        InvestimentoFuturo inv = dao.buscarPorId(id);

        if (inv == null) {
            enviarResposta(exchange, "{\"erro\":\"Investimento não encontrado\"}", 404);
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("id", inv.getId());
        resp.addProperty("nome", inv.getNome());
        resp.addProperty("valorMeta", inv.getValorMeta());
        resp.addProperty("dataAbertura", inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        resp.addProperty("status", inv.getStatus());
        resp.addProperty("saldoAtual", inv.getSaldoAtual());
        enviarResposta(exchange, resp.toString(), 200);
    }

    private void atualizarInvestimento(HttpExchange exchange) throws IOException {
        int id = extrairId(exchange.getRequestURI().getPath());
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        boolean alterandoStatus = body.has("status");
        if (alterandoStatus && !usuarioPodeGerenciar(exchange)) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Apenas administradores podem alterar o status do investimento.\"}", 403);
            return;
        }

        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        InvestimentoFuturo inv = dao.buscarPorId(id);
        if (inv == null) {
            enviarResposta(exchange, "{\"erro\":\"Investimento não encontrado\"}", 404);
            return;
        }

        if (body.has("nome")) inv.setNome(body.get("nome").getAsString());
        if (body.has("valorMeta")) inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
        if (body.has("status")) inv.setStatus(body.get("status").getAsString());

        if (dao.atualizar(inv)) {
            enviarResposta(exchange, "{\"mensagem\":\"Investimento atualizado\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar\"}", 500);
        }
    }

    private void lancarAporte(HttpExchange exchange) throws IOException {
        if (!usuarioTemPermissao(exchange, "LANCAR_APORTE")) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Você não tem permissão para lançar aportes.\"}", 403);
            return;
        }
        int investimentoId = extrairId(exchange.getRequestURI().getPath());
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        BigDecimal valor = body.get("valorAporte").getAsBigDecimal();
        String dataStr = body.get("dataAporte").getAsString();
        int colaboradorId = body.get("colaboradorId").getAsInt();

        InvestimentoFacade facade = new InvestimentoFacade();
        String erro = facade.validarDadosAporte(valor, dataStr);
        if (erro != null) {
            enviarResposta(exchange, "{\"erro\":\"" + erro + "\"}", 400);
            return;
        }

        InvestimentoFuturoDao invDao = new InvestimentoFuturoDao();
        if (invDao.buscarPorId(investimentoId) == null) {
            enviarResposta(exchange, "{\"erro\":\"Investimento não encontrado\"}", 404);
            return;
        }

        AporteInvestimento aporte = new AporteInvestimento();
        aporte.setInvestimentoFuturoId(investimentoId);
        aporte.setValorAporte(valor);
        aporte.setDataAporte(Data.parseFlexivel(dataStr));
        aporte.setColaboradorId(colaboradorId);

        AporteInvestimentoDao apDao = new AporteInvestimentoDao();
        int id = apDao.inserir(aporte);

        if (id > 0) {
            BigDecimal saldo = invDao.calcularSaldo(investimentoId);
            enviarResposta(exchange, "{\"mensagem\":\"Aporte registrado com sucesso\",\"id\":" + id + ",\"saldoAtual\":" + saldo + "}", 201);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao registrar aporte\"}", 500);
        }
    }

    private void listarAportes(HttpExchange exchange) throws IOException {
        if (emailDoToken(exchange) == null) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Faça login.\"}", 401);
            return;
        }
        int investimentoId = extrairId(exchange.getRequestURI().getPath());
        AporteInvestimentoDao dao = new AporteInvestimentoDao();
        List<AporteInvestimento> lista = dao.listarPorInvestimento(investimentoId);

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            AporteInvestimento a = lista.get(i);
            json.append("{");
            json.append("\"id\":").append(a.getId()).append(",");
            json.append("\"valorAporte\":").append(a.getValorAporte()).append(",");
            json.append("\"dataAporte\":\"").append(a.getDataAporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\"");
            json.append("}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        enviarResposta(exchange, json.toString(), 200);
    }

    private boolean usuarioPodeGerenciar(HttpExchange exchange) {
        String email = emailDoToken(exchange);
        if (email == null) return false;
        UsuarioDao uDao = new UsuarioDao();
        Usuario u = uDao.buscarPorEmail(email);
        return u != null && u.getNivelAcesso() == 1;
    }

    private void removerInvestimento(HttpExchange exchange) throws IOException {
        if (!usuarioPodeGerenciar(exchange)) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Apenas administradores podem remover investimentos.\"}", 403);
            return;
        }
        int id = extrairId(exchange.getRequestURI().getPath());
        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        if (dao.deletar(id)) {
            enviarResposta(exchange, "{\"mensagem\":\"Investimento removido\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao remover investimento\"}", 500);
        }
    }

    private void removerAporte(HttpExchange exchange) throws IOException {
        if (!usuarioPodeGerenciar(exchange)) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Apenas administradores podem remover aportes.\"}", 403);
            return;
        }
        String[] partes = exchange.getRequestURI().getPath().split("/");
        int aporteId = Integer.parseInt(partes[partes.length - 1]);
        AporteInvestimentoDao dao = new AporteInvestimentoDao();
        if (dao.deletar(aporteId)) {
            enviarResposta(exchange, "{\"mensagem\":\"Aporte removido\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao remover aporte\"}", 500);
        }
    }

    private int extrairId(String path) {
        String[] partes = path.split("/");
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].matches("\\d+")) return Integer.parseInt(partes[i]);
        }
        return -1;
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void enviarResposta(HttpExchange exchange, String json, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] respostaBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, respostaBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respostaBytes);
        }
    }
}