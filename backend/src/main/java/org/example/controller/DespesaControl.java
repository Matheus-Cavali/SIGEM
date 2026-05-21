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
    public static DespesaControl getInstancia() {
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
            try (Connection conn = Conexao.getConexao()) {
                UsuarioDao uDao = new UsuarioDao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = Conexao.getConexao()) {
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "GERENCIAR_DESPESA");
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return false;
    }

    public Resposta criarCategoriaDespesa(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("nome") || body.get("nome").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Nome da categoria de despesa Ã© obrigatÃ³rio\"}");
        }

        String nome = body.get("nome").getAsString().trim();

        try (Connection conn = Conexao.getConexao()) {
            if (CategoriaDespesa.buscarPorNome(conn, nome) != null) {
                return new Resposta(409, "{\"erro\":\"JÃ¡ existe uma categoria de despesa com este nome\"}");
            }

            CategoriaDespesa categoria = new CategoriaDespesa();
            categoria.setNome(nome);

            int id = CategoriaDespesa.inserir(conn, categoria);
            if (id > 0) return new Resposta(201, "{\"mensagem\":\"Categoria de despesa criada com sucesso\",\"id\":" + id + "}");
            return new Resposta(500, "{\"erro\":\"Erro ao criar categoria de despesa\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao criar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao criar categoria de despesa\"}");
        }
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
        try (Connection conn = Conexao.getConexao()) {
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
        } catch (SQLException e) {
            System.err.println("Erro ao listar categorias de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias de despesa\"}");
        }
    }

    public Resposta atualizarCategoriaDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            CategoriaDespesa categoria = CategoriaDespesa.buscarPorId(conn, id);
            if (categoria == null) return new Resposta(404, "{\"erro\":\"Categoria de despesa nÃ£o encontrada\"}");

            Gson gson = new Gson();
            JsonObject body = gson.fromJson(json, JsonObject.class);
            if (body.has("nome")) {
                String novoNome = body.get("nome").getAsString().trim();
                CategoriaDespesa existente = CategoriaDespesa.buscarPorNome(conn, novoNome);
                if (existente != null && existente.getId() != id) {
                    return new Resposta(409, "{\"erro\":\"JÃ¡ existe outra categoria de despesa com este nome\"}");
                }
                categoria.setNome(novoNome);
            }
            if (CategoriaDespesa.atualizar(conn, categoria)) return new Resposta(200, "{\"mensagem\":\"Categoria de despesa atualizada\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de despesa\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de despesa\"}");
        }
    }

    public Resposta deletarCategoriaDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            if (CategoriaDespesa.buscarPorId(conn, id) == null) return new Resposta(404, "{\"erro\":\"Categoria de despesa nÃ£o encontrada\"}");
            if (CategoriaDespesa.deletar(conn, id)) return new Resposta(200, "{\"mensagem\":\"Categoria de despesa removida\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao remover categoria de despesa. Pode haver despesas vinculadas.\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao deletar categoria de despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao remover categoria de despesa\"}");
        }
    }

    public Resposta lancarDespesa(String auth, String json) {
        if (!usuarioTemPermissao(auth, "LANCAR_DESPESA")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("descricao") || body.get("descricao").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"DescriÃ§Ã£o da despesa Ã© obrigatÃ³ria\"}");
        }
        if (!body.has("valor") || body.get("valor").getAsBigDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            return new Resposta(400, "{\"erro\":\"Valor da despesa deve ser maior que zero\"}");
        }
        if (!body.has("categoriaDespesaId")) {
            return new Resposta(400, "{\"erro\":\"Categoria de despesa Ã© obrigatÃ³ria\"}");
        }
        if (!body.has("dataVencimento") || body.get("dataVencimento").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Data de vencimento Ã© obrigatÃ³ria\"}");
        }

        int categoriaDespesaId = body.get("categoriaDespesaId").getAsInt();
        try (Connection conn = Conexao.getConexao()) {
            if (CategoriaDespesa.buscarPorId(conn, categoriaDespesaId) == null) {
                return new Resposta(404, "{\"erro\":\"Categoria de despesa nÃ£o encontrada\"}");
            }

            String dataVencStr = body.get("dataVencimento").getAsString();
            if (Data.parseFlexivel(dataVencStr) == null) {
                return new Resposta(400, "{\"erro\":\"Data de vencimento invÃ¡lida. Use o formato dd/mm/aaaa\"}");
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

            int id = Despesa.inserir(conn, despesa);
            if (id > 0) return new Resposta(201, "{\"mensagem\":\"Despesa lanÃ§ada com sucesso\",\"id\":" + id + "}");
            return new Resposta(500, "{\"erro\":\"Erro ao lanÃ§ar despesa\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao lanÃ§ar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao lanÃ§ar despesa\"}");
        }
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
        try (Connection conn = Conexao.getConexao()) {
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
                        .append("}");
                if (i < lista.size() - 1) sb.append(",");
            }
            sb.append("]");
            return new Resposta(200, sb.toString());
        } catch (SQLException e) {
            System.err.println("Erro ao listar despesas: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar despesas\"}");
        }
    }

    public Resposta buscarDespesa(int id) {
        try (Connection conn = Conexao.getConexao()) {
            Despesa d = Despesa.buscarPorId(conn, id);
            if (d == null) return new Resposta(404, "{\"erro\":\"Despesa nÃ£o encontrada\"}");
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
        } catch (SQLException e) {
            System.err.println("Erro ao buscar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao buscar despesa\"}");
        }
    }

    public Resposta atualizarDespesa(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            Despesa despesa = Despesa.buscarPorId(conn, id);
            if (despesa == null) return new Resposta(404, "{\"erro\":\"Despesa nÃ£o encontrada\"}");

            Gson gson = new Gson();
            JsonObject body = gson.fromJson(json, JsonObject.class);
            if (body.has("descricao")) despesa.setDescricao(body.get("descricao").getAsString().trim());
            if (body.has("valor")) despesa.setValor(body.get("valor").getAsBigDecimal());
            if (body.has("dataVencimento")) despesa.setDataVencimento(Data.parseFlexivel(body.get("dataVencimento").getAsString()));
            if (body.has("categoriaDespesaId")) despesa.setCategoriaDespesaId(body.get("categoriaDespesaId").getAsInt());

            if (Despesa.atualizar(conn, despesa)) return new Resposta(200, "{\"mensagem\":\"Despesa atualizada\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar despesa\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar despesa\"}");
        }
    }

    public Resposta deletarDespesa(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        try (Connection conn = Conexao.getConexao()) {
            if (Despesa.buscarPorId(conn, id) == null) return new Resposta(404, "{\"erro\":\"Despesa nÃ£o encontrada\"}");
            if (Despesa.deletar(conn, id)) return new Resposta(200, "{\"mensagem\":\"Despesa removida\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao remover despesa\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao deletar despesa: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao remover despesa\"}");
        }
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
