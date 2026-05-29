package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.dao.AporteInvestimentoDao;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.*;
import org.example.util.Data;

import org.example.exception.DatabaseException;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InvestimentoControl {

    private static InvestimentoControl instancia;

    private InvestimentoControl() {}

    public static InvestimentoControl getInstancia() {
        if (instancia == null) instancia = new InvestimentoControl();
        return instancia;
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

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
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro permissão: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = Conexao.getConexao()) {
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO");
            } catch (SQLException ignored) {}
        }
        return false;
    }

    public Resposta registrarInvestimento(String auth, String json) {
        if (!usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(json, JsonObject.class);
            InvestimentoFuturo inv = new InvestimentoFuturo();
            inv.setNome(body.get("nome").getAsString());

            // Lendo o valor diretamente como BigDecimal nativo do JSON
            inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());

            String dataStr = body.get("dataAbertura").getAsString();
            LocalDate dataAbertura = Data.parseFlexivel(dataStr);
            inv.setDataAbertura(dataAbertura);
            inv.setColaboradorId(body.get("colaboradorId").getAsInt());

            Resposta erroData = validarData(dataStr, dataAbertura, "dataAbertura");
            if (erroData != null) return erroData;

            Map<String, String> erros = inv.validar();
            if (!erros.isEmpty()) return new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            if (InvestimentoFuturo.buscarPorNome(conn, inv.getNome().trim()) != null) {
                conn.rollback();
                return new Resposta(409, "{\"erros\":{\"nome\":\"Já existe um investimento com este nome\"}}");
            }

            int id = InvestimentoFuturo.salvar(conn, inv);
            if (id > 0) {
                conn.commit();
                return new Resposta(201, "{\"mensagem\":\"Investimento registrado com sucesso\",\"id\":" + id + "}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao registrar investimento\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao registrar investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    public Resposta listarInvestimentos(String auth, String query) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        try (Connection conn = Conexao.getConexao()) {
            String nome = null, status = null;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2 && "nome".equalsIgnoreCase(kv[0])) nome = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (kv.length == 2 && "status".equalsIgnoreCase(kv[0])) status = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
            List<InvestimentoFuturo> lista = new InvestimentoFuturoDao().listar(conn, nome, status);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar investimentos. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta buscarInvestimento(String auth, int id) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");
        try (Connection conn = Conexao.getConexao()) {
            InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, id);
            if (inv == null) return new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
            return new Resposta(200, gson.toJson(inv));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar investimento. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta atualizarInvestimento(String auth, int id, String json) {
        JsonObject body = gson.fromJson(json, JsonObject.class);
        if (body.has("status") && !usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);
            InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, id);

            if (inv == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
            }

            if (body.has("nome")) inv.setNome(body.get("nome").getAsString());
            if (body.has("valorMeta")) inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
            if (body.has("status")) inv.setStatus(body.get("status").getAsString());

            if (InvestimentoFuturo.atualizar(conn, inv)) {
                conn.commit();
                return new Resposta(200, "{\"mensagem\":\"Investimento atualizado\"}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar investimento\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    public Resposta lancarAporte(String auth, int investimentoId, String json) {
        if (!usuarioTemPermissao(auth, "LANCAR_APORTE")) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(json, JsonObject.class);
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, investimentoId);
            if (inv == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
            } else if ("ENCERRADO".equals(inv.getStatus())) {
                conn.rollback();
                return new Resposta(400, "{\"erro\":\"Investimento encerrado. Não é permitido lançar aportes.\"}");
            }

            AporteInvestimento aporte = new AporteInvestimento();
            aporte.setInvestimentoFuturoId(investimentoId);

            // Lendo nativamente
            aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());

            String dataAporteStr = body.get("dataAporte").getAsString();
            LocalDate dataAporte = Data.parseFlexivel(dataAporteStr);
            aporte.setDataAporte(dataAporte);
            aporte.setColaboradorId(body.get("colaboradorId").getAsInt());

            Resposta erroData = validarData(dataAporteStr, dataAporte, "dataAporte");
            if (erroData != null) return erroData;

            Map<String, String> erros = aporte.validar();
            if (!erros.isEmpty()) {
                conn.rollback();
                return new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
            }

            int id = AporteInvestimento.salvar(conn, aporte);
            if (id > 0) {
                BigDecimal saldo = InvestimentoFuturo.calcularSaldo(conn, investimentoId);
                conn.commit();
                return new Resposta(201, "{\"mensagem\":\"Aporte registrado com sucesso\",\"id\":" + id + ",\"saldoAtual\":" + saldo + "}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao registrar aporte\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao lançar aporte. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    public Resposta listarAportes(String auth, int investimentoId) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");
        try (Connection conn = Conexao.getConexao()) {
            List<AporteInvestimento> lista = new AporteInvestimentoDao().listarPorInvestimento(conn, investimentoId);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar aportes. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta removerInvestimento(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);
            new AporteInvestimentoDao().deletarPorInvestimento(conn, id);

            if (InvestimentoFuturo.deletar(conn, id)) {
                conn.commit();
                return new Resposta(200, "{\"mensagem\":\"Investimento removido\"}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao remover investimento\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao remover investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    public Resposta removerAporte(String auth, int aporteId) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);
            if (AporteInvestimento.deletar(conn, aporteId)) {
                conn.commit();
                return new Resposta(200, "{\"mensagem\":\"Aporte removido\"}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao remover aporte\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao remover aporte. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    public Resposta atualizarAporte(String auth, int aporteId, String json) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(json, JsonObject.class);
            AporteInvestimento aporte = new AporteInvestimento();

            // Lendo nativamente
            aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());

            String dataAporteStr = body.get("dataAporte").getAsString();
            LocalDate dataAporte = Data.parseFlexivel(dataAporteStr);
            aporte.setDataAporte(dataAporte);

            Resposta erroData = validarData(dataAporteStr, dataAporte, "dataAporte");
            if (erroData != null) return erroData;

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            if (AporteInvestimento.atualizar(conn, aporteId, aporte)) {
                conn.commit();
                return new Resposta(200, "{\"mensagem\":\"Aporte atualizado\"}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar aporte\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar aporte: " + e.getMessage() + "\"}");
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) {}
        }
    }

    private String mensagemErroUsuario(Exception e) {
        SQLException sqlCause = null;

        if (e instanceof DatabaseException && e.getCause() instanceof SQLException) {
            sqlCause = (SQLException) e.getCause();
        } else if (e instanceof SQLException) {
            sqlCause = (SQLException) e;
        }

        if (sqlCause != null) {
            String state = sqlCause.getSQLState();
            if ("23503".equals(state))
                return "Não é possível remover pois existem registros vinculados a este item. Exclua os vínculos primeiro.";
            if ("23505".equals(state))
                return "Já existe um registro com estes mesmos dados no sistema.";
            if ("08001".equals(state) || "08S01".equals(state))
                return "Erro de conexão com o banco de dados. Verifique se o servidor está ativo.";
        }

        if (e instanceof DatabaseException) {
            String msg = e.getMessage();
            if (msg != null && !msg.isEmpty()) return msg;
        }

        return e.getMessage() != null ? e.getMessage() : "Erro interno inesperado. Tente novamente.";
    }

    private Resposta validarData(String dataStr, LocalDate data, String fieldName) {
        if (dataStr != null && !dataStr.trim().isEmpty() && data == null) {
            String limpa = dataStr.replaceAll("[^0-9]", "");
            if (limpa.length() == 8) {
                int dia = Integer.parseInt(limpa.substring(0, 2));
                int mes = Integer.parseInt(limpa.substring(2, 4));
                if (mes < 1 || mes > 12) {
                    return new Resposta(400, "{\"erros\":{\"" + fieldName + "\":\"Mês inválido. Deve estar entre 1 e 12\"}}");
                }
                if (dia < 1 || dia > 31) {
                    return new Resposta(400, "{\"erros\":{\"" + fieldName + "\":\"Dia inválido. Deve estar entre 1 e 31\"}}");
                }
            }
            return new Resposta(400, "{\"erros\":{\"" + fieldName + "\":\"Data inválida\"}}");
        }
        return null;
    }
}