package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;

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

    private static InvestimentoFuturo investimentoFuturo;
    private static AporteInvestimento aporteInvestimento;

    public static synchronized InvestimentoFuturo getInvestimentoFuturo() {
        if (investimentoFuturo == null) investimentoFuturo = new InvestimentoFuturo();
        return investimentoFuturo;
    }

    public static synchronized AporteInvestimento getAporteInvestimento() {
        if (aporteInvestimento == null) aporteInvestimento = new AporteInvestimento();
        return aporteInvestimento;
    }

    public InvestimentoControl() {}

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
            try {
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if (u != null) {
                    if (u.getNivelAcesso() == 1) return true;
                    for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                        if (r.getNome().equals(recursoNome)) return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        String email = emailDoToken(auth);
        if (email != null) {
            try {
                Connection conn = Conexao.getConexao();
                Usuario u = Usuario.buscarPorEmail(conn, email);
                if (u != null) return u.getNivelAcesso() == 1 || usuarioTemPermissao(auth, "REGISTRAR_INVESTIMENTO");
            } catch (Exception ignored) {}
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

            if (body.get("nome") == null || body.get("valorMeta") == null || body.get("dataAbertura") == null || body.get("colaboradorId") == null) {
                return new Resposta(400, "{\"erro\":\"Campos obrigatórios: nome, valorMeta, dataAbertura, colaboradorId\"}");
            }

            InvestimentoFuturo inv = new InvestimentoFuturo();
            inv.setNome(body.get("nome").getAsString());
            inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
            String dataStr = body.get("dataAbertura").getAsString();
            LocalDate dataAbertura = Data.parseFlexivel(dataStr);
            inv.setDataAbertura(dataAbertura);
            inv.setColaboradorId(body.get("colaboradorId").getAsInt());

            Resposta erroData = validarData(dataStr, dataAbertura, "dataAbertura");
            if (erroData != null) return erroData;
            if (dataAbertura != null && dataAbertura.isBefore(LocalDate.now()))
                return new Resposta(400, "{\"erros\":{\"dataAbertura\":\"Não é permitido lançar investimento com data anterior à data atual\"}}");

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            getInvestimentoFuturo().cadastrar(conn, inv);
            conn.commit();
            return new Resposta(201, "{\"mensagem\":\"Investimento registrado com sucesso\",\"id\":" + inv.getId() + "}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao registrar investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta listarInvestimentos(String auth, String query) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        try {
            Connection conn = Conexao.getConexao();
            String nome = null, status = null;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2 && "nome".equalsIgnoreCase(kv[0])) nome = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (kv.length == 2 && "status".equalsIgnoreCase(kv[0])) status = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
            List<InvestimentoFuturo> lista = getInvestimentoFuturo().filtrar(conn, nome, status);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar investimentos. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta buscarInvestimento(String auth, int id) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");
        try {
            Connection conn = Conexao.getConexao();
            InvestimentoFuturo inv = getInvestimentoFuturo().buscarPorId(conn, id);
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
            InvestimentoFuturo inv = getInvestimentoFuturo().buscarPorId(conn, id);

            if (inv == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
            }

            if (body.has("nome")) inv.setNome(body.get("nome").getAsString());
            if (body.has("valorMeta")) inv.setValorMeta(body.get("valorMeta").getAsBigDecimal());
            if (body.has("status")) inv.setStatus(body.get("status").getAsString());
            if (body.has("dataAbertura")) {
                inv.setDataAbertura(Data.parseFlexivel(body.get("dataAbertura").getAsString()));
            }

            getInvestimentoFuturo().alterar(conn, inv);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Investimento atualizado\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta lancarAporte(String auth, int investimentoId, String json) {
        if (!usuarioTemPermissao(auth, "LANCAR_APORTE")) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(json, JsonObject.class);
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            if (body.get("valorAporte") == null || body.get("dataAporte") == null || body.get("colaboradorId") == null) {
                conn.rollback();
                return new Resposta(400, "{\"erro\":\"Campos obrigatórios: valorAporte, dataAporte, colaboradorId\"}");
            }

            InvestimentoFuturo inv = getInvestimentoFuturo().buscarPorId(conn, investimentoId);
            if (inv == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Investimento não encontrado\"}");
            } else if ("ENCERRADO".equals(inv.getStatus())) {
                conn.rollback();
                return new Resposta(400, "{\"erro\":\"Investimento encerrado. Não é permitido lançar aportes.\"}");
            }

            AporteInvestimento aporte = new AporteInvestimento();
            aporte.setInvestimentoFuturoId(investimentoId);
            aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());

            String dataAporteStr = body.get("dataAporte").getAsString();
            LocalDate dataAporte = Data.parseFlexivel(dataAporteStr);
            aporte.setDataAporte(dataAporte);
            int colaboradorId = body.get("colaboradorId").getAsInt();
            aporte.setColaboradorId(colaboradorId);

            Resposta erroData = validarData(dataAporteStr, dataAporte, "dataAporte");
            if (erroData != null) return erroData;
            if (dataAporte != null && dataAporte.isBefore(LocalDate.now()))
                return new Resposta(400, "{\"erros\":{\"dataAporte\":\"Não é permitido lançar aporte com data anterior à data atual\"}}");

            if (dataAporte != null && inv.getDataAbertura() != null && dataAporte.isBefore(inv.getDataAbertura())) {
                boolean antecipar = body.has("anteciparData") && body.get("anteciparData").getAsBoolean();
                if (!antecipar) {
                    conn.rollback();
                    return new Resposta(400, "{\"erro\":\"A data do aporte é anterior à data de abertura do investimento. É necessário antecipar a data de abertura.\"}");
                }
                inv.setDataAbertura(dataAporte);
                getInvestimentoFuturo().alterar(conn, inv);
            }

            Caixa caixaHelper = new Caixa();
            Caixa caixaAberto = caixaHelper.buscarCaixaAberto(conn);
            boolean autoCriouCaixa = false;

            if (caixaAberto == null) {
                Caixa novoCaixa = new Caixa();
                novoCaixa.setDataCaixa(LocalDate.now());
                novoCaixa.setHorarioAbertura(java.time.LocalTime.now());
                BigDecimal ultimoFechamento = BigDecimal.ZERO;
                Caixa ultimo = Caixa.getDao().buscarUltimoCaixa(conn);
                if (ultimo != null && ultimo.getValorFechamento() != null)
                    ultimoFechamento = ultimo.getValorFechamento();
                novoCaixa.setValorAbertura(ultimoFechamento);
                novoCaixa.setSaldo(ultimoFechamento);
                novoCaixa.setColaboradorAbriuId(colaboradorId);
                Caixa.getDao().inserir(conn, novoCaixa);
                caixaAberto = novoCaixa;
                autoCriouCaixa = true;
            }

            aporte.setCaixaId(caixaAberto.getId());

            getAporteInvestimento().cadastrar(conn, aporte);

            caixaHelper.movimentar(conn, caixaAberto.getId(), aporte.getValorAporte());

            if (autoCriouCaixa) {
                Caixa caixaAtualizado = caixaHelper.buscarPorId(conn, caixaAberto.getId());
                Caixa paraFechar = new Caixa();
                paraFechar.setId(caixaAberto.getId());
                paraFechar.setValorFechamento(caixaAtualizado.getSaldo());
                paraFechar.setColaboradorFechouId(colaboradorId);
                caixaHelper.fechar(conn, paraFechar);
            }

            BigDecimal saldo = getInvestimentoFuturo().calcularSaldo(conn, investimentoId);

            conn.commit();

            JsonObject resp = new JsonObject();
            resp.addProperty("mensagem", "Aporte registrado com sucesso");
            resp.addProperty("id", aporte.getId());
            resp.addProperty("saldoAtual", saldo);
            resp.addProperty("caixaId", caixaAberto.getId());

            if (saldo.compareTo(inv.getValorMeta()) > 0) {
                String aviso = "Valor total de aportes (" + String.format("%.2f", saldo) +
                        ") ultrapassou o valor previsto (" + String.format("%.2f", inv.getValorMeta()) + ")";
                resp.addProperty("aviso", aviso);
            }

            return new Resposta(201, resp.toString());
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao lançar aporte. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta listarAportes(String auth, int investimentoId) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");
        try {
            Connection conn = Conexao.getConexao();
            List<AporteInvestimento> lista = getAporteInvestimento().filtrarPorInvestimento(conn, investimentoId);
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
            getAporteInvestimento().excluirPorInvestimento(conn, id);
            getInvestimentoFuturo().excluir(conn, id);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Investimento removido\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao remover investimento. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta removerAporte(String auth, int aporteId) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            AporteInvestimento aporte = getAporteInvestimento().buscarPorId(conn, aporteId);
            if (aporte == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Aporte não encontrado\"}");
            }

            if (aporte.getCaixaId() != null) {
                Caixa caixaHelper = new Caixa();
                caixaHelper.movimentar(conn, aporte.getCaixaId(), aporte.getValorAporte().negate());
            }

            getAporteInvestimento().excluir(conn, aporteId);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Aporte removido\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao remover aporte. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta atualizarAporte(String auth, int aporteId, String json) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(json, JsonObject.class);

            if (body.get("valorAporte") == null || body.get("dataAporte") == null) {
                return new Resposta(400, "{\"erro\":\"Campos obrigatórios: valorAporte, dataAporte\"}");
            }

            AporteInvestimento aporte = new AporteInvestimento();
            aporte.setValorAporte(body.get("valorAporte").getAsBigDecimal());

            String dataAporteStr = body.get("dataAporte").getAsString();
            LocalDate dataAporte = Data.parseFlexivel(dataAporteStr);
            aporte.setDataAporte(dataAporte);

            Resposta erroData = validarData(dataAporteStr, dataAporte, "dataAporte");
            if (erroData != null) return erroData;
            if (dataAporte != null && dataAporte.isBefore(LocalDate.now()))
                return new Resposta(400, "{\"erros\":{\"dataAporte\":\"Não é permitido atualizar aporte com data anterior à data atual\"}}");

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            getAporteInvestimento().alterar(conn, aporteId, aporte);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Aporte atualizado\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar aporte: " + e.getMessage() + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
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
