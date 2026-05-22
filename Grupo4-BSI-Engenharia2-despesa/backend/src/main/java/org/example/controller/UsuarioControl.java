package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.dao.VoluntarioDao;
import org.example.model.*;
import org.example.util.CpfUtil;
import org.example.util.Criptografia;
import org.example.util.Data;
import org.example.util.Token;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class UsuarioControl {

    private static UsuarioControl instancia;
    private UsuarioControl() {}
    public static UsuarioControl getInstancia() {
        if (instancia == null) instancia = new UsuarioControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = Token.validarToken(auth.substring(7));
        }
        return token;
    }

    private int nivelAcessoDoToken(String auth) {
        int nivel = -1;
        if (auth != null && auth.startsWith("Bearer ")) {
            nivel = Token.extrairNivelAcesso(auth.substring(7));
        }
        return nivel;
    }

    private boolean usuarioTemPermissaoDb(String auth, String recursoNome) {
        boolean permitido = false;
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (Token.extrairNivelAcesso(token) == 1) {
                permitido = true;
            } else {
                String email = Token.validarToken(token);
                if (email != null) {
                    try (Connection conn = Conexao.getConexao()) {
                        Usuario u = Usuario.buscarPorEmail(conn, email);
                        if (u != null) {
                            RecursoSistemaDao rDao = new RecursoSistemaDao();
                            List<RecursoSistema> permissoes = rDao.listarPorUsuario(conn, u.getId());
                            for (RecursoSistema r : permissoes) {
                                if (!permitido && r.getNome().equals(recursoNome)) {
                                    permitido = true;
                                }
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Erro ao verificar permissao: " + e.getMessage());
                    }
                }
            }
        }
        return permitido;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        boolean pode = false;
        if (auth != null && auth.startsWith("Bearer ")) {
            if (Token.extrairNivelAcesso(auth.substring(7)) == 1) {
                pode = true;
            } else {
                pode = usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS");
            }
        }
        return pode;
    }

    public Resposta processarLogin(String jsonRecebido) {
        Resposta result;
        try {
            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
                result = new Resposta(400, "{\"erro\":\"Corpo da requisiÃ§Ã£o vazio\"}");
            } else {
                Gson gson = new Gson();
                Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);

                if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getEmail().trim().isEmpty()) {
                    result = new Resposta(400, "{\"erro\":\"Email ou CPF Ã© obrigatÃ³rio\"}");
                } else {
                    Connection conn = Conexao.getConexao();
                    UsuarioDao dao = new UsuarioDao();
                    Usuario usuarioDoBanco;
                    String loginInput = usuarioLogin.getEmail().trim();

                    if (loginInput.contains("@")) {
                        usuarioDoBanco = dao.buscarPorEmail(conn, loginInput);
                    } else {
                        String cpfLimpo = loginInput.replaceAll("[^0-9]", "");
                        usuarioDoBanco = cpfLimpo.length() == 11 ? dao.buscarPorCPF(conn, cpfLimpo) : null;
                    }

                    if (usuarioDoBanco == null || !Criptografia.verificarSenha(usuarioLogin.getSenha(), usuarioDoBanco.getSenha())) {
                        String campo = loginInput.contains("@") ? "E-mail" : "CPF";
                        result = new Resposta(401, "{\"erro\":\"" + campo + " ou senha incorretos\"}");
                    } else if (!usuarioDoBanco.isStatusAtivo()) {
                        result = new Resposta(403, "{\"erro\":\"UsuÃ¡rio desativado. Contate um administrador.\"}");
                    } else if ("voluntario".equalsIgnoreCase(usuarioDoBanco.getTipoUsuario())) {
                        result = new Resposta(403, "{\"erro\":\"VoluntÃ¡rios nÃ£o podem acessar o sistema. Apenas colaboradores.\"}");
                    } else if (usuarioDoBanco.isPrimeiroAcesso()) {
                        JsonObject resp = new JsonObject();
                        resp.addProperty("status", "TROCA_OBRIGATORIA");
                        resp.addProperty("mensagem", "Primeiro acesso detectado. Altere a sua senha.");
                        resp.addProperty("cpf", usuarioDoBanco.getCpf());
                        result = new Resposta(200, resp.toString());
                    } else {
                        RecursoSistemaDao recDao = new RecursoSistemaDao();
                        List<RecursoSistema> permissoes = usuarioDoBanco.getNivelAcesso() == 1
                            ? recDao.listarTodos(conn) : recDao.listarPorUsuario(conn, usuarioDoBanco.getId());

                        JsonArray permArray = new JsonArray();
                        for (RecursoSistema r : permissoes) permArray.add(r.getNome());

                        String token = Token.gerarToken(usuarioDoBanco.getEmail(), usuarioDoBanco.getNivelAcesso(), usuarioDoBanco.getTipoUsuario());
                        JsonObject resp = new JsonObject();
                        resp.addProperty("token", token);
                        resp.addProperty("mensagem", "Login realizado!");
                        resp.addProperty("nivelAcesso", usuarioDoBanco.getNivelAcesso());
                        resp.addProperty("statusAtivo", usuarioDoBanco.isStatusAtivo());
                        resp.addProperty("tipoUsuario", usuarioDoBanco.getTipoUsuario());
                        resp.addProperty("nome", usuarioDoBanco.getNome());
                        resp.addProperty("email", usuarioDoBanco.getEmail());
                        resp.addProperty("id", usuarioDoBanco.getId());
                        resp.add("permissoes", permArray);
                        result = new Resposta(200, resp.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ERRO no processarLogin: " + e.getMessage());
            result = new Resposta(500, "{\"erro\":\"Falha ao processar login. Verifique os dados e tente novamente.\"}");
        }
        return result;
    }

    public Resposta processarAlteracaoSenha(String jsonRecebido) {
        Resposta result;
        try {
            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
                result = new Resposta(400, "{\"erro\":\"Corpo da requisiÃ§Ã£o vazio\"}");
            } else {
                Gson gson = new Gson();
                Usuario dadosNovos = gson.fromJson(jsonRecebido, Usuario.class);
                if (dadosNovos == null || dadosNovos.getCpf() == null || dadosNovos.getSenha() == null || dadosNovos.getSenha().trim().isEmpty()) {
                    result = new Resposta(400, "{\"erro\":\"CPF e nova senha sÃ£o obrigatÃ³rios\"}");
                } else if (dadosNovos.getSenha().length() < 4) {
                    result = new Resposta(400, "{\"erro\":\"Senha deve ter no mÃ­nimo 4 caracteres\"}");
                } else {
                    try (Connection conn = Conexao.getConexao()) {
                        String cpfLimpo = dadosNovos.getCpf().replaceAll("[^0-9]", "");

                        UsuarioDao dao = new UsuarioDao();
                        if (dao.buscarPorCPF(conn, cpfLimpo) == null) {
                            result = new Resposta(404, "{\"erro\":\"CPF nÃ£o encontrado no sistema\"}");
                        } else {
                            boolean sucesso = dao.mudarSenha(conn, cpfLimpo, Criptografia.hashSenha(dadosNovos.getSenha()));
                            if (sucesso) {
                                result = new Resposta(200, "{\"mensagem\":\"Senha alterada! FaÃ§a login novamente.\"}");
                            } else {
                                result = new Resposta(500, "{\"erro\":\"Erro ao atualizar a senha.\"}");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ERRO no processarAlteracaoSenha: " + e.getMessage());
            result = new Resposta(500, "{\"erro\":\"Falha ao alterar senha. Tente novamente.\"}");
        }
        return result;
    }

    public Resposta processarCadastroInterno(String jsonRecebido, String auth) {
        Resposta result;
        String email = emailDoToken(auth);
        if (email == null) {
            result = new Resposta(401, "{\"erro\":\"Acesso negado. FaÃ§a login.\"}");
        } else if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            result = new Resposta(400, "{\"erro\":\"Corpo da requisiÃ§Ã£o vazio\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                Usuario dados = gson.fromJson(jsonRecebido, Usuario.class);
                if (dados == null || dados.getEmail() == null || dados.getSenha() == null || dados.getTipoUsuario() == null) {
                    result = new Resposta(400, "{\"erro\":\"Dados invÃ¡lidos. Email, senha e tipo sÃ£o obrigatÃ³rios.\"}");
                } else {
                    Map<String, String> erros = dados.validar();
                    if (!erros.isEmpty()) {
                        result = new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));
                    } else {
                        conn = Conexao.getConexao();
                        conn.setAutoCommit(false);

                        UsuarioDao uDao = new UsuarioDao();
                        Usuario quemCadastra = uDao.buscarPorEmail(conn, email);
                        boolean isAdmin = quemCadastra != null && quemCadastra.getNivelAcesso() == 1;

                        String tipo = dados.getTipoUsuario().trim();
                        boolean podeCadastrar = isAdmin
                            || usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS")
                            || ("colaborador".equalsIgnoreCase(tipo) && usuarioTemPermissaoDb(auth, "GESTAO_COLABORADORES"))
                            || ("voluntario".equalsIgnoreCase(tipo) && usuarioTemPermissaoDb(auth, "GESTAO_VOLUNTARIOS"));

                        if (!podeCadastrar) {
                            conn.rollback();
                            result = new Resposta(403, "{\"erro\":\"Acesso negado. VocÃª nÃ£o tem permissÃ£o para cadastrar usuÃ¡rios.\"}");
                        } else {
                            String senhaBanco = Criptografia.hashSenha(dados.getSenha());
                            String cpfLimpo = dados.getCpf() != null ? dados.getCpf().replaceAll("[^0-9]", "") : "";

                            if (uDao.buscarPorEmail(conn, dados.getEmail()) != null) {
                                conn.rollback();
                                result = new Resposta(409, "{\"erros\":{\"email\":\"Email jÃ¡ cadastrado no sistema\"}}");
                            } else if (uDao.buscarPorCPF(conn, cpfLimpo) != null) {
                                conn.rollback();
                                result = new Resposta(409, "{\"erros\":{\"cpf\":\"CPF jÃ¡ cadastrado no sistema\"}}");
                            } else {
                                int idGerado = Usuario.cadastrar(conn, dados.getNome(), dados.getEmail(), senhaBanco, cpfLimpo, dados.getNivelAcesso(), tipo);

                                if ("colaborador".equalsIgnoreCase(tipo)) {
                                    Colaborador.cadastrar(conn, idGerado, dados.getData());
                                } else if ("voluntario".equalsIgnoreCase(tipo)) {
                                    Voluntario.cadastrar(conn, idGerado, dados.getData());
                                }

                                conn.commit();
                                result = new Resposta(201, "{\"mensagem\":\"Cadastro realizado!\"}");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("ERRO no processarCadastroInterno: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao cadastrar usuÃ¡rio: " + e.getMessage() + "\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta listarUsuarios(String auth, String query) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            String nome = null, emailParam = null, tipo = null;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        if ("email".equalsIgnoreCase(kv[0])) emailParam = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        if ("tipo".equalsIgnoreCase(kv[0])) tipo = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }

            try (Connection conn = Conexao.getConexao()) {
                UsuarioDao dao = new UsuarioDao();
                List<Usuario> usuarios = dao.listarTodos(conn, nome, emailParam, tipo);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < usuarios.size(); i++) {
                    Usuario u = usuarios.get(i);
                    json.append("{\"id\":").append(u.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(u.getNome())).append("\",");
                    json.append("\"email\":\"").append(escaparJson(u.getEmail())).append("\",");
                    json.append("\"cpf\":\"").append(escaparJson(u.getCpf())).append("\",");
                    json.append("\"rg\":\"").append(escaparJson(u.getRg())).append("\",");
                    json.append("\"celular\":\"").append(escaparJson(u.getCelular())).append("\",");
                    json.append("\"rua\":\"").append(escaparJson(u.getRua())).append("\",");
                    json.append("\"bairro\":\"").append(escaparJson(u.getBairro())).append("\",");
                    json.append("\"cep\":\"").append(escaparJson(u.getCep())).append("\",");
                    json.append("\"cidade\":\"").append(escaparJson(u.getCidade())).append("\",");
                    json.append("\"estado\":\"").append(escaparJson(u.getEstado())).append("\",");
                    json.append("\"nivelAcesso\":").append(u.getNivelAcesso()).append(",");
                    json.append("\"statusAtivo\":").append(u.isStatusAtivo()).append(",");
                    json.append("\"tipoUsuario\":\"").append(escaparJson(u.getTipoUsuario())).append("\",");
                    json.append("\"primeiroAcesso\":").append(u.isPrimeiroAcesso());
                    json.append("}");
                    if (i < usuarios.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar usuarios: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar usuÃ¡rios.\"}");
            }
        }
        return result;
    }

    public Resposta buscarUsuario(String auth, int id) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = Conexao.getConexao()) {
                UsuarioDao dao = new UsuarioDao();
                Usuario u = dao.buscarPorId(conn, id);
                if (u == null) {
                    result = new Resposta(404, "{\"erro\":\"UsuÃ¡rio nÃ£o encontrado\"}");
                } else {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"id\":").append(u.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(u.getNome())).append("\",");
                    json.append("\"email\":\"").append(escaparJson(u.getEmail())).append("\",");
                    json.append("\"cpf\":\"").append(escaparJson(u.getCpf())).append("\",");
                    json.append("\"rg\":\"").append(escaparJson(u.getRg())).append("\",");
                    json.append("\"celular\":\"").append(escaparJson(u.getCelular())).append("\",");
                    json.append("\"rua\":\"").append(escaparJson(u.getRua())).append("\",");
                    json.append("\"bairro\":\"").append(escaparJson(u.getBairro())).append("\",");
                    json.append("\"cep\":\"").append(escaparJson(u.getCep())).append("\",");
                    json.append("\"cidade\":\"").append(escaparJson(u.getCidade())).append("\",");
                    json.append("\"estado\":\"").append(escaparJson(u.getEstado())).append("\",");
                    json.append("\"nivelAcesso\":").append(u.getNivelAcesso()).append(",");
                    json.append("\"statusAtivo\":").append(u.isStatusAtivo()).append(",");
                    json.append("\"tipoUsuario\":\"").append(escaparJson(u.getTipoUsuario())).append("\",");
                    json.append("\"primeiroAcesso\":").append(u.isPrimeiroAcesso());
                    json.append("}");
                    result = new Resposta(200, json.toString());
                }
            } catch (SQLException e) {
                System.err.println("Erro ao buscar usuario: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao buscar usuÃ¡rio.\"}");
            }
        }
        return result;
    }

    public Resposta atualizarUsuario(String auth, int id, String jsonBody) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(jsonBody, JsonObject.class);

                conn = Conexao.getConexao();
                conn.setAutoCommit(false);

                UsuarioDao dao = new UsuarioDao();
                Usuario u = dao.buscarPorId(conn, id);
                if (u == null) {
                    conn.rollback();
                    result = new Resposta(404, "{\"erro\":\"UsuÃ¡rio nÃ£o encontrado\"}");
                } else {
                    if (body.has("nome")) u.setNome(body.get("nome").getAsString());
                    if (body.has("email")) u.setEmail(body.get("email").getAsString());
                    if (body.has("cpf")) u.setCpf(body.get("cpf").getAsString().replaceAll("[^0-9]", ""));
                    if (body.has("rg")) u.setRg(body.get("rg").isJsonNull() ? null : body.get("rg").getAsString());
                    if (body.has("celular")) u.setCelular(body.get("celular").isJsonNull() ? null : body.get("celular").getAsString());
                    if (body.has("rua")) u.setRua(body.get("rua").isJsonNull() ? null : body.get("rua").getAsString());
                    if (body.has("bairro")) u.setBairro(body.get("bairro").isJsonNull() ? null : body.get("bairro").getAsString());
                    if (body.has("cep")) u.setCep(body.get("cep").isJsonNull() ? null : body.get("cep").getAsString());
                    if (body.has("cidade")) u.setCidade(body.get("cidade").isJsonNull() ? null : body.get("cidade").getAsString());
                    if (body.has("estado")) u.setEstado(body.get("estado").isJsonNull() ? null : body.get("estado").getAsString());
                    if (body.has("nivelAcesso")) u.setNivelAcesso(body.get("nivelAcesso").getAsInt());
                    if (body.has("tipoUsuario")) u.setTipoUsuario(body.get("tipoUsuario").getAsString());

                    if (dao.atualizar(conn, u)) {
                        conn.commit();
                        result = new Resposta(200, "{\"mensagem\":\"UsuÃ¡rio atualizado com sucesso\"}");
                    } else {
                        conn.rollback();
                        result = new Resposta(500, "{\"erro\":\"Erro ao atualizar usuÃ¡rio\"}");
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao atualizar usuario: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar usuÃ¡rio: " + e.getMessage() + "\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta alterarStatusUsuario(String auth, int id, String jsonBody) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
                boolean ativo = body.get("status_ativo").getAsBoolean();

                conn = Conexao.getConexao();
                conn.setAutoCommit(false);

                UsuarioDao dao = new UsuarioDao();
                Usuario usuario = dao.buscarPorId(conn, id);
                if (usuario == null) {
                    conn.rollback();
                    result = new Resposta(404, "{\"erro\":\"UsuÃ¡rio nÃ£o encontrado\"}");
                } else if (!ativo && "colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
                    int totalAtivos = dao.contarColaboradorAcessoTotalAtivo(conn);
                    if (totalAtivos <= 1) {
                        conn.rollback();
                        result = new Resposta(400, "{\"erro\":\"NÃ£o Ã© possÃ­vel desativar o Ãºnico colaborador com acesso total\"}");
                    } else {
                        result = processarAlteracaoStatus(conn, dao, id, usuario, ativo, body);
                    }
                } else {
                    result = processarAlteracaoStatus(conn, dao, id, usuario, ativo, body);
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao alterar status: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao alterar status do usuÃ¡rio.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    private Resposta processarAlteracaoStatus(Connection conn, UsuarioDao dao, int id, Usuario usuario, boolean ativo, JsonObject body) throws SQLException {
        Resposta result;
        if (dao.alterarStatus(conn, id, ativo)) {
            String tipo = usuario.getTipoUsuario();
            String dataStr = body.has("data_desligamento") && !body.get("data_desligamento").isJsonNull()
                ? body.get("data_desligamento").getAsString() : null;

            LocalDate dataDesligamento = null;
            if (!ativo && dataStr != null) {
                dataDesligamento = "SYSDATE".equalsIgnoreCase(dataStr) ? LocalDate.now() : Data.parseFlexivel(dataStr);
            }

            if ("colaborador".equalsIgnoreCase(tipo)) {
                new ColaboradorDao().atualizarDataDemissao(conn, id, !ativo ? dataDesligamento : null);
            } else if ("voluntario".equalsIgnoreCase(tipo)) {
                new VoluntarioDao().atualizarDataDesligamento(conn, id, !ativo ? dataDesligamento : null);
            }
            conn.commit();
            result = new Resposta(200, "{\"mensagem\":\"Status atualizado com sucesso\"}");
        } else {
            conn.rollback();
            result = new Resposta(500, "{\"erro\":\"Erro ao atualizar status\"}");
        }
        return result;
    }

    public Resposta removerUsuario(String auth, int id) {
        Resposta result;
        if (!usuarioPodeGerenciar(auth)) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                conn = Conexao.getConexao();
                conn.setAutoCommit(false);

                UsuarioDao dao = new UsuarioDao();
                Usuario usuario = dao.buscarPorId(conn, id);
                if (usuario == null) {
                    conn.rollback();
                    result = new Resposta(404, "{\"erro\":\"UsuÃ¡rio nÃ£o encontrado\"}");
                } else if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
                    if (dao.contarColaboradorAcessoTotalAtivo(conn) <= 1) {
                        conn.rollback();
                        result = new Resposta(400, "{\"erro\":\"NÃ£o Ã© possÃ­vel remover o Ãºnico colaborador com acesso total\"}");
                    } else {
                        if (dao.deletar(conn, id)) {
                            conn.commit();
                            result = new Resposta(200, "{\"mensagem\":\"UsuÃ¡rio removido com sucesso\"}");
                        } else {
                            conn.rollback();
                            result = new Resposta(500, "{\"erro\":\"Erro ao remover usuÃ¡rio\"}");
                        }
                    }
                } else {
                    if (dao.deletar(conn, id)) {
                        conn.commit();
                        result = new Resposta(200, "{\"mensagem\":\"UsuÃ¡rio removido com sucesso\"}");
                    } else {
                        conn.rollback();
                        result = new Resposta(500, "{\"erro\":\"Erro ao remover usuÃ¡rio\"}");
                    }
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao remover usuario: " + e.getMessage());
                String mensagem;
                if (e instanceof SQLException && "23503".equals(((SQLException) e).getSQLState())) {
                    mensagem = "NÃ£o Ã© possÃ­vel remover este usuÃ¡rio pois existem registros vinculados a ele (investimentos, lanÃ§amentos etc.). Exclua os vÃ­nculos primeiro.";
                } else {
                    mensagem = "Falha ao remover usuÃ¡rio.";
                }
                result = new Resposta(500, "{\"erro\":\"" + mensagem + "\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
        }
        return result;
    }

    public Resposta listarTodosRecursos(String auth) {
        Resposta result;
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = Conexao.getConexao()) {
                RecursoSistemaDao dao = new RecursoSistemaDao();
                List<RecursoSistema> lista = dao.listarTodos(conn);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    RecursoSistema r = lista.get(i);
                    json.append("{\"id\":").append(r.getId()).append(",");
                    json.append("\"nome\":\"").append(escaparJson(r.getNome())).append("\",");
                    json.append("\"descricao\":\"").append(escaparJson(r.getDescricao())).append("\"}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");
                result = new Resposta(200, json.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar recursos: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar recursos.\"}");
            }
        }
        return result;
    }

    public Resposta listarPermissoes(String auth, int usuarioId) {
        Resposta result;
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            try (Connection conn = Conexao.getConexao()) {
                RecursoSistemaDao dao = new RecursoSistemaDao();
                List<RecursoSistema> permissoes = dao.listarPorUsuario(conn, usuarioId);
                JsonArray arr = new JsonArray();
                for (RecursoSistema r : permissoes) arr.add(r.getNome());
                JsonObject resp = new JsonObject();
                resp.addProperty("usuarioId", usuarioId);
                resp.add("permissoes", arr);
                result = new Resposta(200, resp.toString());
            } catch (SQLException e) {
                System.err.println("Erro ao listar permissoes: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao listar permissÃµes.\"}");
            }
        }
        return result;
    }

    public Resposta atualizarPermissoes(String auth, int usuarioId, String jsonBody) {
        Resposta result;
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) {
            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        } else {
            Connection conn = null;
            try {
                Gson gson = new Gson();
                JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
                JsonArray arr = body.getAsJsonArray("recursoIds");
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) ids.add(arr.get(i).getAsInt());

                conn = Conexao.getConexao();
                conn.setAutoCommit(false);

                RecursoSistemaDao dao = new RecursoSistemaDao();
                if (dao.atualizarPermissoes(conn, usuarioId, ids)) {
                    conn.commit();
                    result = new Resposta(200, "{\"mensagem\":\"PermissÃµes atualizadas\"}");
                } else {
                    conn.rollback();
                    result = new Resposta(500, "{\"erro\":\"Erro ao atualizar permissÃµes\"}");
                }
            } catch (Exception e) {
                if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Erro no rollback: " + ex.getMessage()); }
                System.err.println("Erro ao atualizar permissoes: " + e.getMessage());
                result = new Resposta(500, "{\"erro\":\"Falha ao atualizar permissÃµes.\"}");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException e) { System.err.println("Erro ao fechar conexao: " + e.getMessage()); }
            }
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
