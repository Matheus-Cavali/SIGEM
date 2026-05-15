package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.dao.DespesaDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.CategoriaDespesaDao;
import org.example.dao.UsuarioDao;
import org.example.model.*;
import org.example.util.Data;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DespesaControl {

    private static DespesaControl instancia;
    private DespesaControl() {}
    public static DespesaControl getInstancia() {
        if (instancia == null) instancia = new DespesaControl();
        return instancia;
    }

    // ─── Helpers de autenticação (mesmo padrão do CInvestimento) ─────────────

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
            if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "GERENCIAR_DESPESA");
        }
        return false;
    }

    // ─── Categoria de Despesa ────────────────────────────────────────────────

    public Resposta criarCategoriaDespesa(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("nome") || body.get("nome").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Nome da categoria de despesa é obrigatório\"}");
        }

        String nome = body.get("nome").getAsString().trim();

        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        if (dao.buscarPorNome(nome) != null) {
            return new Resposta(409, "{\"erro\":\"Já existe uma categoria de despesa com este nome\"}");
        }

        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setNome(nome);

        int id = dao.inserir(categoria);
        if (id > 0) return new Resposta(201, "{\"mensagem\":\"Categoria de despesa criada com sucesso\",\"id\":" + id + "}");
        return new Resposta(500, "{\"erro\":\"Erro ao criar categoria de despesa\"}");
    }

    public Resposta listarCategoriasDespesa(String query) {
        String nome = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        List<CategoriaDespesa> lista = new CategoriaDespesaDao().listar(nome);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            CategoriaDespesa t = lista.get(i);
            sb.append("{\"id\":").append(t.getId())
                    .append(",\"nome\":\"").append(escaparJson(t.getNome())).append("\"}");
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");
        return new Resposta(200, sb.toString());
    }

    public Resposta atualizarCategoriaDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        CategoriaDespesa categoria = dao.buscarPorId(id);
        if (categoria == null) return new Resposta(404, "{\"erro\":\"Categoria de despesa não encontrada\"}");

        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        if (body.has("nome")) {
            String novoNome = body.get("nome").getAsString().trim();
            CategoriaDespesa existente = dao.buscarPorNome(novoNome);
            if (existente != null && existente.getId() != id) {
                return new Resposta(409, "{\"erro\":\"Já existe outra categoria de despesa com este nome\"}");
            }
            categoria.setNome(novoNome);
        }
        if (dao.atualizar(categoria)) return new Resposta(200, "{\"mensagem\":\"Categoria de despesa atualizada\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de despesa\"}");
    }

    public Resposta deletarCategoriaDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        CategoriaDespesaDao dao = new CategoriaDespesaDao();
        if (dao.buscarPorId(id) == null) return new Resposta(404, "{\"erro\":\"Categoria de despesa não encontrada\"}");
        if (dao.deletar(id)) return new Resposta(200, "{\"mensagem\":\"Categoria de despesa removida\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao remover categoria de despesa. Pode haver despesas vinculadas.\"}");
    }

    // ─── Despesas ─────────────────────────────────────────────────────────────

    public Resposta lancarDespesa(String auth, String json) {
        if (!usuarioTemPermissao(auth, "LANCAR_DESPESA")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("descricao") || body.get("descricao").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Descrição da despesa é obrigatória\"}");
        }
        if (!body.has("valor") || body.get("valor").getAsBigDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            return new Resposta(400, "{\"erro\":\"Valor da despesa deve ser maior que zero\"}");
        }
        if (!body.has("categoriaDespesaId")) {
            return new Resposta(400, "{\"erro\":\"Categoria de despesa é obrigatória\"}");
        }
        if (!body.has("dataVencimento") || body.get("dataVencimento").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Data de vencimento é obrigatória\"}");
        }

        int categoriaDespesaId = body.get("categoriaDespesaId").getAsInt();
        if (new CategoriaDespesaDao().buscarPorId(categoriaDespesaId) == null) {
            return new Resposta(404, "{\"erro\":\"Categoria de despesa não encontrada\"}");
        }

        String dataVencStr = body.get("dataVencimento").getAsString();
        if (Data.parseFlexivel(dataVencStr) == null) {
            return new Resposta(400, "{\"erro\":\"Data de vencimento inválida. Use o formato dd/mm/aaaa\"}");
        }

        Despesa despesa = new Despesa();
        despesa.setDescricao(body.get("descricao").getAsString().trim());
        despesa.setValor(body.get("valor").getAsBigDecimal());
        despesa.setCategoriaDespesaId(categoriaDespesaId);
        despesa.setDataVencimento(Data.parseFlexivel(dataVencStr));
        despesa.setDataLancamento(java.time.LocalDate.now());

        if (body.has("colaboradorLancouId") && !body.get("colaboradorLancouId").isJsonNull()) {
            despesa.setColaboradorLancouId(body.get("colaboradorLancouId").getAsInt());
        }

        int id = new DespesaDao().inserir(despesa);
        if (id > 0) return new Resposta(201, "{\"mensagem\":\"Despesa lançada com sucesso\",\"id\":" + id + "}");
        return new Resposta(500, "{\"erro\":\"Erro ao lançar despesa\"}");
    }

    public Resposta listarDespesas(String query) {
        String descricao = null;
        Integer tipoId = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    String chave = kv[0];
                    String valor = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if ("descricao".equalsIgnoreCase(chave)) descricao = valor;
                    if ("categoriaDespesaId".equalsIgnoreCase(chave)) tipoId = Integer.parseInt(valor);
                }
            }
        }
        List<Despesa> lista = new DespesaDao().listar(descricao, tipoId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            Despesa d = lista.get(i);
            sb.append("{\"id\":").append(d.getId())
                    .append(",\"descricao\":\"").append(escaparJson(d.getDescricao())).append("\"")
                    .append(",\"valor\":").append(d.getValor())
                    .append(",\"dataLancamento\":\"").append(d.getDataLancamento() != null ? d.getDataLancamento().format(fmt) : "").append("\"")
                    .append(",\"dataVencimento\":\"").append(d.getDataVencimento() != null ? d.getDataVencimento().format(fmt) : "").append("\"")
                    .append(",\"dataPagamento\":").append(d.getDataPagamento() != null ? "\"" + d.getDataPagamento().format(fmt) + "\"" : "null")
                    .append(",\"categoriaDespesaId\":").append(d.getCategoriaDespesaId())
                    .append(",\"categoriaDespesaNome\":\"").append(escaparJson(d.getCategoriaDespesaNome())).append("\"")
                    .append("}");
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");
        return new Resposta(200, sb.toString());
    }

    public Resposta buscarDespesa(int id) {
        Despesa d = new DespesaDao().buscarPorId(id);
        if (d == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String json = "{\"id\":" + d.getId()
                + ",\"descricao\":\"" + escaparJson(d.getDescricao()) + "\""
                + ",\"valor\":" + d.getValor()
                + ",\"dataLancamento\":\"" + (d.getDataLancamento() != null ? d.getDataLancamento().format(fmt) : "") + "\""
                + ",\"dataVencimento\":\"" + (d.getDataVencimento() != null ? d.getDataVencimento().format(fmt) : "") + "\""
                + ",\"dataPagamento\":" + (d.getDataPagamento() != null ? "\"" + d.getDataPagamento().format(fmt) + "\"" : "null")
                + ",\"categoriaDespesaId\":" + d.getCategoriaDespesaId()
                + ",\"categoriaDespesaNome\":\"" + escaparJson(d.getCategoriaDespesaNome()) + "\""
                + "}";
        return new Resposta(200, json);
    }

    public Resposta atualizarDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        DespesaDao dao = new DespesaDao();
        Despesa despesa = dao.buscarPorId(id);
        if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");

        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        if (body.has("descricao")) despesa.setDescricao(body.get("descricao").getAsString().trim());
        if (body.has("valor")) despesa.setValor(body.get("valor").getAsBigDecimal());
        if (body.has("dataVencimento")) despesa.setDataVencimento(Data.parseFlexivel(body.get("dataVencimento").getAsString()));
        if (body.has("categoriaDespesaId")) despesa.setCategoriaDespesaId(body.get("categoriaDespesaId").getAsInt());

        if (dao.atualizar(despesa)) return new Resposta(200, "{\"mensagem\":\"Despesa atualizada\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar despesa\"}");
    }

    public Resposta deletarDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        DespesaDao dao = new DespesaDao();
        if (dao.buscarPorId(id) == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");
        if (dao.deletar(id)) return new Resposta(200, "{\"mensagem\":\"Despesa removida\"}");
        return new Resposta(500, "{\"erro\":\"Erro ao remover despesa\"}");
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
