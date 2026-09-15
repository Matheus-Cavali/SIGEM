package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.*;
import org.example.util.Data;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DespesaControl {

    private static DespesaControl instancia;
    private DespesaControl() {}
    public static synchronized DespesaControl getInstancia() {
        if (instancia == null) instancia = new DespesaControl();
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
            try {
                Connection conn = Conexao.getConexao();
                UsuarioDao uDao = new UsuarioDao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            try {
                Connection conn = Conexao.getConexao();
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "GESTAO_DESPESAS");
            } catch (Exception e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    // ── Categorias de Despesa ─────────────────────────────────────────────────

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

        Connection conn = Conexao.getConexao();
        try {
            if (CategoriaDespesa.buscarPorNome(conn, nome) != null) {
                return new Resposta(409, "{\"erro\":\"Já existe uma categoria de despesa com este nome\"}");
            }

            CategoriaDespesa categoria = new CategoriaDespesa();
            categoria.setNome(nome);

            int id = CategoriaDespesa.inserir(conn, categoria);
            if (id > 0) return new Resposta(201, "{\"mensagem\":\"Categoria de despesa criada com sucesso\",\"id\":" + id + "}");
            return new Resposta(500, "{\"erro\":\"Erro ao criar categoria de despesa\"}");
        } catch (Exception e) {
            System.err.println("Erro ao criar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao criar categoria de despesa\"}");
        }
    }

    public Resposta listarCategoriasDespesa(String auth, String query) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        String nome = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0]))
                        nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        Connection conn = Conexao.getConexao();
        try {
            List<CategoriaDespesa> lista = CategoriaDespesa.listar(conn, nome);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                CategoriaDespesa t = lista.get(i);
                sb.append("{\"id\":").append(t.getId())
                        .append(",\"nome\":\"").append(escaparJson(t.getNome())).append("\"}");
                if (i < lista.size() - 1) sb.append(",");
            }
            sb.append("]");
            return new Resposta(200, sb.toString());
        } catch (Exception e) {
            System.err.println("Erro ao listar categorias de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias de despesa\"}");
        }
    }

    public Resposta atualizarCategoriaDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            CategoriaDespesa categoria = CategoriaDespesa.buscarPorId(conn, id);
            if (categoria == null) return new Resposta(404, "{\"erro\":\"Categoria de despesa não encontrada\"}");

            Gson gson = new Gson();
            JsonObject body = gson.fromJson(json, JsonObject.class);
            if (body.has("nome")) {
                String novoNome = body.get("nome").getAsString().trim();
                CategoriaDespesa existente = CategoriaDespesa.buscarPorNome(conn, novoNome);
                if (existente != null && existente.getId() != id) {
                    return new Resposta(409, "{\"erro\":\"Já existe outra categoria de despesa com este nome\"}");
                }
                categoria.setNome(novoNome);
            }
            if (CategoriaDespesa.atualizar(conn, categoria))
                return new Resposta(200, "{\"mensagem\":\"Categoria de despesa atualizada\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de despesa\"}");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de despesa\"}");
        }
    }

    public Resposta deletarCategoriaDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            if (CategoriaDespesa.buscarPorId(conn, id) == null)
                return new Resposta(404, "{\"erro\":\"Categoria de despesa não encontrada\"}");
            if (CategoriaDespesa.deletar(conn, id))
                return new Resposta(200, "{\"mensagem\":\"Categoria de despesa removida\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao remover categoria de despesa. Pode haver despesas vinculadas.\"}");
        } catch (Exception e) {
            System.err.println("Erro ao deletar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao remover categoria de despesa\"}");
        }
    }

    // ── Despesas ──────────────────────────────────────────────────────────────

    public Resposta lancarDespesa(String auth, String json) {
        if (!usuarioTemPermissao(auth, "GESTAO_DESPESAS") && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        Connection conn = Conexao.getConexao();
        try {
            Gson gson = new Gson();
            JsonObject body = gson.fromJson(json, JsonObject.class);

            Despesa despesa = new Despesa();

            if (body.has("descricao") && !body.get("descricao").isJsonNull())
                despesa.setDescricao(body.get("descricao").getAsString().trim());
            if (body.has("valor") && !body.get("valor").isJsonNull())
                despesa.setValor(body.get("valor").getAsBigDecimal());
            if (body.has("categoriaDespesaId") && !body.get("categoriaDespesaId").isJsonNull())
                despesa.setCategoriaDespesaId(body.get("categoriaDespesaId").getAsInt());
            if (body.has("dataVencimento") && !body.get("dataVencimento").isJsonNull()) {
                despesa.setDataVencimento(Data.parseFlexivel(body.get("dataVencimento").getAsString().trim()));
            }
            if (body.has("dataPrazo") && !body.get("dataPrazo").isJsonNull()) {
                String dp = body.get("dataPrazo").getAsString().trim();
                if (!dp.isEmpty()) despesa.setDataPrazo(Data.parseFlexivel(dp));
            }
            if (body.has("colaboradorLancouId") && !body.get("colaboradorLancouId").isJsonNull())
                despesa.setColaboradorLancouId(body.get("colaboradorLancouId").getAsInt());

            // Usa método de instância com validação completa (padrão Material)
            Despesa instancia = new Despesa();
            instancia.cadastrar(conn, despesa);

            return new Resposta(201, "{\"mensagem\":\"Despesa lançada com sucesso\"}");

        } catch (RuntimeException e) {
            // Propaga mapa de erros por campo para o frontend (padrão Material)
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) {
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"" + escaparJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            System.err.println("Erro ao lançar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao lançar despesa\"}");
        }
    }

    public Resposta listarDespesas(String auth, String query) {
        if (!usuarioTemPermissao(auth, "GESTAO_DESPESAS") && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
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
        Connection conn = Conexao.getConexao();
        try {
            List<Despesa> lista = Despesa.listar(conn, descricao, tipoId);
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
                        .append(",\"dataPrazo\":").append(d.getDataPrazo() != null ? "\"" + d.getDataPrazo().format(fmt) + "\"" : "null")
                        .append(",\"valorPago\":").append(d.getValorPago() != null ? d.getValorPago() : "null")
                        .append(",\"emAtraso\":").append(d.isEmAtraso())
                        .append(",\"valorComJuros\":").append(d.calcularValorComJuros())
                        .append("}");
                if (i < lista.size() - 1) sb.append(",");
            }
            sb.append("]");
            return new Resposta(200, sb.toString());
        } catch (Exception e) {
            System.err.println("Erro ao listar despesas:");
            e.printStackTrace();
            return new Resposta(500, "{\"erro\":\"" + e.getMessage() + "\"}");
        }
    }

    public Resposta buscarDespesa(String auth, int id) {
        if (!usuarioTemPermissao(auth, "GESTAO_DESPESAS") && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            Despesa d = Despesa.buscarPorId(conn, id);
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
        } catch (Exception e) {
            System.err.println("Erro ao buscar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao buscar despesa\"}");
        }
    }

    public Resposta atualizarDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            Despesa despesa = Despesa.buscarPorId(conn, id);
            if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");

            Gson gson = new Gson();
            JsonObject body = gson.fromJson(json, JsonObject.class);
            if (body.has("descricao")) despesa.setDescricao(body.get("descricao").getAsString().trim());
            if (body.has("valor")) despesa.setValor(body.get("valor").getAsBigDecimal());
            if (body.has("dataVencimento")) despesa.setDataVencimento(Data.parseFlexivel(body.get("dataVencimento").getAsString()));
            if (body.has("dataPrazo")) {
                String dp = body.get("dataPrazo").isJsonNull() ? "" : body.get("dataPrazo").getAsString().trim();
                despesa.setDataPrazo(dp.isEmpty() ? null : Data.parseFlexivel(dp));
            }
            if (body.has("categoriaDespesaId")) despesa.setCategoriaDespesaId(body.get("categoriaDespesaId").getAsInt());

            // Usa método de instância com validação (padrão Material)
            Despesa inst = new Despesa();
            inst.alterar(conn, despesa);

            return new Resposta(200, "{\"mensagem\":\"Despesa atualizada com sucesso\"}");

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) {
                return new Resposta(400, msg);
            }
            return new Resposta(400, "{\"erro\":\"" + escaparJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar despesa\"}");
        }
    }

    public Resposta deletarDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            Despesa despesa = Despesa.buscarPorId(conn, id);
            if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");
            // Usa método de instância com validação de ID (padrão Material)
            Despesa inst = new Despesa();
            inst.excluir(conn, id);
            return new Resposta(200, "{\"mensagem\":\"Despesa excluída com sucesso\"}");
        } catch (RuntimeException e) {
            return new Resposta(400, "{\"erro\":\"" + escaparJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            System.err.println("Erro ao deletar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao remover despesa\"}");
        }
    }

    public Resposta quitarDespesa(String auth, int id, String json) {
        if (!usuarioTemPermissao(auth, "GESTAO_DESPESAS") && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            Despesa despesa = Despesa.buscarPorId(conn, id);
            if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");
            if (despesa.getDataPagamento() != null) return new Resposta(409, "{\"erro\":\"Despesa já foi quitada\"}");

            java.time.LocalDate dataPagamento = java.time.LocalDate.now();
            if (json != null && !json.isBlank()) {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(json, JsonObject.class);
                if (body.has("dataPagamento") && !body.get("dataPagamento").isJsonNull()) {
                    String dp = body.get("dataPagamento").getAsString().trim();
                    if (!dp.isEmpty()) {
                        dataPagamento = Data.parseFlexivel(dp);
                        if (dataPagamento == null)
                            return new Resposta(400, "{\"erro\":\"Data de pagamento inválida\"}");
                    }
                }
            }

            // Calcula se incide juros baseado na data de pagamento vs prazo da despesa
            java.math.BigDecimal valorFinal = despesa.calcularValorComJuros(dataPagamento);
            boolean comJuros = valorFinal.compareTo(despesa.getValor()) > 0;

            if (Despesa.quitar(conn, id, dataPagamento, valorFinal)) {
                String msg = comJuros
                    ? "Despesa quitada com juros compostos de 2% ao dia. Valor cobrado: " + valorFinal
                    : "Despesa quitada com sucesso";
                return new Resposta(200, "{\"mensagem\":\"" + escaparJson(msg) + "\",\"valorPago\":" + valorFinal + ",\"comJuros\":" + comJuros + "}");
            }
            return new Resposta(500, "{\"erro\":\"Erro ao quitar despesa\"}");
        } catch (Exception e) {
            System.err.println("Erro ao quitar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao quitar despesa\"}");
        }
    }

    public Resposta consultarSaldo(String auth) {
        if (!usuarioTemPermissao(auth, "GESTAO_DESPESAS") && !usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            BigDecimal saldo = Despesa.calcularSaldo(conn);
            return new Resposta(200, "{\"saldo\":" + saldo + "}");
        } catch (Exception e) {
            System.err.println("Erro ao consultar saldo: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao consultar saldo\"}");
        }
    }

    public Resposta estornarDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            Despesa despesa = Despesa.buscarPorId(conn, id);
            if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa não encontrada\"}");
            if (despesa.getDataPagamento() == null) return new Resposta(409, "{\"erro\":\"Despesa ainda não foi paga\"}");
            if (Despesa.estornar(conn, id))
                return new Resposta(200, "{\"mensagem\":\"Pagamento estornado com sucesso\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao estornar pagamento\"}");
        } catch (Exception e) {
            System.err.println("Erro ao estornar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao estornar pagamento\"}");
        }
    }

    public Resposta ajustarSaldo(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = Conexao.getConexao();
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonObject body = gson.fromJson(json, com.google.gson.JsonObject.class);
            if (!body.has("valor") || body.get("valor").isJsonNull())
                return new Resposta(400, "{\"erro\":\"Valor é obrigatório\"}");
            java.math.BigDecimal valor = body.get("valor").getAsBigDecimal();
            if (valor.compareTo(java.math.BigDecimal.ZERO) < 0)
                return new Resposta(400, "{\"erro\":\"Valor não pode ser negativo\"}");
            if (Despesa.ajustarSaldo(conn, valor))
                return new Resposta(200, "{\"mensagem\":\"Saldo ajustado com sucesso\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao ajustar saldo\"}");
        } catch (Exception e) {
            System.err.println("Erro ao ajustar saldo: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao ajustar saldo\"}");
        }
    }

        private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
