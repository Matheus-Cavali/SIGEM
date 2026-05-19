package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.ConexaoSingleton;
import org.example.dao.AporteInvestimentoDao;
import org.example.dao.InvestimentoFuturoDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.*;
import org.example.util.Data;

import java.math.BigDecimal;
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

    private String emailDoToken(String auth) {
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = org.example.util.Token.validarToken(auth.substring(7));
        }
        return token;
    }

    private boolean usuarioTemPermissao(String auth, String recursoNome) {
        boolean permitido = false;
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                UsuarioDao uDao = new UsuarioDao();
                Usuario u = uDao.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) {
                        permitido = true;
                    } else {
                        RecursoSistemaDao rDao = new RecursoSistemaDao();
                        for (RecursoSistema r : rDao.listarPorUsuario(conn, u.getId())) {
                            if (!permitido && r.getNome().equals(recursoNome)) {
                                permitido = true;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return permitido;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        boolean pode = false;
        String email = emailDoToken(auth);
        if (email != null) {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                Usuario u = new UsuarioDao().buscarPorEmail(conn, email);
                if (u != null) {
                    pode = u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO");
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar permissao: " + e.getMessage());
            }
        }
        return pode;
    }

    public Resposta registrarInvestimento(String auth, String json) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(json, JsonObject.class);
                String nome = body.get("nome").getAsString();
                BigDecimal meta = body.get("valorMeta").getAsBigDecimal();
                String dataStr = body.get("dataAbertura").getAsString();
                int colaboradorId = body.get("colaboradorId").getAsInt();

                InvestimentoFuturo inv = new InvestimentoFuturo();
                inv.setNome(nome);
                inv.setValorMeta(meta);
                LocalDate dataAbertura = Data.parseFlexivel(dataStr);
                inv.setDataAbertura(dataAbertura);
                inv.setColaboradorId(colaboradorId);

                Resposta erroData = validarDataAbertura(dataStr, dataAbertura);
                if (erroData != null) {
                    result = erroData;
                } else {
                    Map<String, String> erros = inv.validar();
                    if (!erros.isEmpty()) {
                        result = new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
                    } else {
                        conn = ConexaoSingleton.getInstance().getConexao();
                        conn.setAutoCommit(false);

                        InvestimentoFuturo existente = InvestimentoFuturo.buscarPorNome(conn, nome.trim());
                        if (existente != null) {
                            conn.rollback();
                            Map<String, String> err = new LinkedHashMap<>();
                            err.put("nome", "Já existe um investimento com este nome");
                            result = new Resposta(409, gson.toJson(Collections.singletonMap("erros", err)));
                        } else {
                            int id = InvestimentoFuturo.salvar(conn, inv);
                            if (id > 0) {
                                conn.commit();
                                result = new Resposta(201, "{\"mensagem\":\"Investimento registrado com sucesso\",\"id\":" + id + "}");
                            } else {
                                conn.rollback();
                                result = new Resposta(500, "{\"erro\":\"Erro ao registrar investimento\"}");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("ERRO no registrarInvestimento: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao registrar investimento: " + e.getMessage() + "\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta listarInvestimentos(String auth, String query) {
        Resposta result;
        if (emailDoToken(auth) == null) {
            result = new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        } else {
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
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
                List<InvestimentoFuturo> lista = dao.listar(conn, nome, status);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    InvestimentoFuturo inv = lista.get(i);
                    json.append("{\"id\":").append(inv.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(inv.getNome())).append("\",");
                    json.append("\"valorMeta\":").append(inv.getValorMeta()).append(",");
                    json.append("\"dataAbertura\":\"").append(inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\",");
                    json.append("\"status\":\"").append(inv.getStatus()).append("\",");
                    json.append("\"saldoAtual\":").append(inv.getSaldoAtual()).append(",");
                    json.append("\"colaboradorNome\":\"").append(escaparJson(inv.getColaboradorNome())).append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar investimentos: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar investimentos.\"}");
            }
        }
        return result;
    }

    public Resposta buscarInvestimento(String auth, int id) {
        Resposta result;
        if (emailDoToken(auth) == null) {
            result = new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        } else {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, id);
                if (inv == null) {
                    result = new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
                } else {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", inv.getId()); resp.addProperty("nome", inv.getNome());
                    resp.addProperty("valorMeta", inv.getValorMeta());
                    resp.addProperty("dataAbertura", inv.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    resp.addProperty("status", inv.getStatus()); resp.addProperty("saldoAtual", inv.getSaldoAtual());
                    resp.addProperty("colaboradorNome", inv.getColaboradorNome());
                    result = new Resposta(200, resp.toString());
                }
            } catch (SQLException e) {
                System.err.println("Erro ao buscar investimento: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao buscar investimento.\"}");
            }
        }
        return result;
    }

    public Resposta atualizarInvestimento(String auth, int id, String json) {
        Resposta result;
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        boolean alterandoStatus = body.has("status");
        if (alterandoStatus && !usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                conn = ConexaoSingleton.getInstance().getConexao();
                conn.setAutoCommit(false);

                InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, id);
                if (inv == null) {
                    conn.rollback();
                    result = new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
                } else {
                    if (body.has("nome")) inv.setNome(body.get("nome").getAsString());
                    if (body.has("valorMeta")) inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
                    if (body.has("status")) inv.setStatus(body.get("status").getAsString());

                    if (InvestimentoFuturo.atualizar(conn, inv)) {
                        conn.commit();
                        result = new Resposta(200, "{\"mensagem\":\"Investimento atualizado\"}");
                    } else {
                        conn.rollback();
                        result = new Resposta(500, "{\"erro\":\"Erro ao atualizar investimento\"}");
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao atualizar investimento: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar investimento.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta lancarAporte(String auth, int investimentoId, String json) {
        Resposta result;
        if (!usuarioTemPermissao(auth, "LANCAR_APORTE")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(json, JsonObject.class);
                BigDecimal valor = body.get("valorAporte").getAsBigDecimal();
                String dataStr = body.get("dataAporte").getAsString();
                int colaboradorId = body.get("colaboradorId").getAsInt();

                conn = ConexaoSingleton.getInstance().getConexao();
                conn.setAutoCommit(false);

                InvestimentoFuturo inv = InvestimentoFuturo.buscarPorId(conn, investimentoId);
                if (inv == null) {
                    conn.rollback();
                    result = new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
                } else if ("ENCERRADO".equals(inv.getStatus())) {
                    conn.rollback();
                    result = new Resposta(400, "{\"erro\":\"Investimento encerrado. Não é permitido lançar aportes.\"}");
                } else {
                    AporteInvestimento aporte = new AporteInvestimento();
                    aporte.setInvestimentoFuturoId(investimentoId);
                    aporte.setValorAporte(valor);
                    aporte.setDataAporte(Data.parseFlexivel(dataStr));
                    aporte.setColaboradorId(colaboradorId);

                    Map<String, String> erros = aporte.validar();
                    if (!erros.isEmpty()) {
                        conn.rollback();
                        result = new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
                    } else {
                        int id = AporteInvestimento.salvar(conn, aporte);
                        if (id > 0) {
                            BigDecimal saldo = InvestimentoFuturo.calcularSaldo(conn, investimentoId);
                            conn.commit();
                            result = new Resposta(201, "{\"mensagem\":\"Aporte registrado com sucesso\",\"id\":" + id + ",\"saldoAtual\":" + saldo + "}");
                        } else {
                            conn.rollback();
                            result = new Resposta(500, "{\"erro\":\"Erro ao registrar aporte\"}");
                        }
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("ERRO no lancarAporte: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao lançar aporte: " + e.getMessage() + "\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta listarAportes(String auth, int investimentoId) {
        Resposta result;
        if (emailDoToken(auth) == null) {
            result = new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        } else {
            try (Connection conn = ConexaoSingleton.getInstance().getConexao()) {
                AporteInvestimentoDao dao = new AporteInvestimentoDao();
                List<AporteInvestimento> lista = dao.listarPorInvestimento(conn, investimentoId);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    AporteInvestimento a = lista.get(i);
                    json.append("{\"id\":").append(a.getId()).append(",");
                    json.append("\"valorAporte\":").append(a.getValorAporte()).append(",");
                    json.append("\"dataAporte\":\"").append(a.getDataAporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\",");
                    json.append("\"colaboradorNome\":\"").append(escaparJson(a.getColaboradorNome())).append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar aportes: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar aportes.\"}");
            }
        }
        return result;
    }

    public Resposta removerInvestimento(String auth, int id) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                conn = ConexaoSingleton.getInstance().getConexao();
                conn.setAutoCommit(false);

                new AporteInvestimentoDao().deletarPorInvestimento(conn, id);
                if (InvestimentoFuturo.deletar(conn, id)) {
                    conn.commit();
                    result = new Resposta(200, "{\"mensagem\":\"Investimento removido\"}");
                } else {
                    conn.rollback();
                    result = new Resposta(500, "{\"erro\":\"Erro ao remover investimento\"}");
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao remover investimento: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao remover investimento.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta removerAporte(String auth, int aporteId) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                conn = ConexaoSingleton.getInstance().getConexao();
                conn.setAutoCommit(false);

                if (AporteInvestimento.deletar(conn, aporteId)) {
                    conn.commit();
                    result = new Resposta(200, "{\"mensagem\":\"Aporte removido\"}");
                } else {
                    conn.rollback();
                    result = new Resposta(500, "{\"erro\":\"Erro ao remover aporte\"}");
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao remover aporte: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao remover aporte.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta atualizarAporte(String auth, int aporteId, String json) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(json, JsonObject.class);
                AporteInvestimento aporte = new AporteInvestimento();
                aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());
                aporte.setDataAporte(Data.parseFlexivel(body.get("dataAporte").getAsString()));

                conn = ConexaoSingleton.getInstance().getConexao();
                conn.setAutoCommit(false);

                if (AporteInvestimento.atualizar(conn, aporteId, aporte)) {
                    conn.commit();
                    result = new Resposta(200, "{\"mensagem\":\"Aporte atualizado\"}");
                } else {
                    conn.rollback();
                    result = new Resposta(500, "{\"erro\":\"Erro ao atualizar aporte\"}");
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao atualizar aporte: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar aporte.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    private Resposta validarDataAbertura(String dataStr, LocalDate dataAbertura) {
        Resposta result = null;
        if (dataStr != null && !dataStr.trim().isEmpty() && dataAbertura == null) {
            result = new Resposta(400, "{\"erros\":{\"dataAbertura\":\"Data de abertura inválida. Use o formato dd/mm/aaaa\"}}");
        }
        return result;
    }

    private String escaparJson(String s) {
        String res;
        if (s == null) {
            res = "";
        } else {
            res = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
        return res;
    }
}
