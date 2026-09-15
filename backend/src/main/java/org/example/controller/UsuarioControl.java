package org.example.controller;

import com.google.gson.*;
import org.example.conexao.Conexao;
import org.example.exception.DatabaseException;
import org.example.model.*;
import org.example.util.Criptografia;
import org.example.util.Data;
import org.example.util.Token;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UsuarioControl {

    private static Usuario usuario;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            .create();

    public static synchronized Usuario getUsuario() {
        if (usuario == null) usuario = new Usuario();
        return usuario;
    }

    private static UsuarioControl instancia;

    private UsuarioControl() {}

    public static synchronized UsuarioControl getInstancia() {
        if (instancia == null) instancia = new UsuarioControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) return Token.validarToken(auth.substring(7));
        return null;
    }

    private boolean usuarioTemPermissaoDb(String auth, String recursoNome) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (Token.extrairNivelAcesso(token) == 1) return true;

            String email = Token.validarToken(token);
            if (email != null) {
                try {
                    Connection conn = Conexao.getConexao();
                    Usuario u = Usuario.buscarPorEmail(conn, email);
                    if (u != null) {
                        for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {
                            if (r.getNome().equals(recursoNome)) return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return Token.extrairNivelAcesso(auth.substring(7)) == 1 || usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS");
        }
        return false;
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
            if ("23505".equals(state))
                return "Já existe um registro com estes mesmos dados no sistema.";
            if ("23503".equals(state))
                return "Não é possível remover pois existem registros vinculados a este item. Exclua os vínculos primeiro.";
            if ("08001".equals(state) || "08S01".equals(state))
                return "Erro de conexão com o banco de dados. Verifique se o servidor está ativo.";
        }
        if (e instanceof DatabaseException) {
            String msg = e.getMessage();
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return e.getMessage() != null ? e.getMessage() : "Erro interno inesperado. Tente novamente.";
    }

    public Resposta processarLogin(String jsonRecebido) {
        try {
            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) return new Resposta(400, "{\"erro\":\"Corpo da requisição vazio\"}");

            Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);
            if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getEmail().trim().isEmpty()) {
                return new Resposta(400, "{\"erro\":\"Email ou CPF é obrigatório\"}");
            }

            Connection conn = Conexao.getConexao();
            Usuario usuarioDoBanco;
            String loginInput = usuarioLogin.getEmail().trim();

            if (loginInput.contains("@")) {
                usuarioDoBanco = Usuario.buscarPorEmail(conn, loginInput);
            } else {
                String cpfLimpo = loginInput.replaceAll("[^0-9]", "");
                usuarioDoBanco = cpfLimpo.length() == 11 ? Usuario.buscarPorCPF(conn, cpfLimpo) : null;
            }

            if (usuarioDoBanco == null || !Criptografia.verificarSenha(usuarioLogin.getSenha(), usuarioDoBanco.getSenha())) {
                String campo = loginInput.contains("@") ? "E-mail" : "CPF";
                return new Resposta(401, "{\"erro\":\"" + campo + " ou senha incorretos\"}");
            } else if (!usuarioDoBanco.isStatusAtivo()) {
                return new Resposta(403, "{\"erro\":\"Usuário desativado. Contate um administrador.\"}");
            } else if ("voluntario".equalsIgnoreCase(usuarioDoBanco.getTipoUsuario())) {
                return new Resposta(403, "{\"erro\":\"Voluntários não podem acessar o sistema. Apenas colaboradores.\"}");
            } else if (usuarioDoBanco.isPrimeiroAcesso()) {
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "TROCA_OBRIGATORIA");
                resp.addProperty("mensagem", "Primeiro acesso detectado. Altere a sua senha.");
                resp.addProperty("cpf", usuarioDoBanco.getCpf());
                return new Resposta(200, resp.toString());
            } else {
                List<RecursoSistema> permissoes = usuarioDoBanco.getNivelAcesso() == 1
                        ? RecursoSistema.listarTodos(conn) : RecursoSistema.listarPorUsuario(conn, usuarioDoBanco.getId());

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

                return new Resposta(200, resp.toString());
            }
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao processar login. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta processarAlteracaoSenha(String jsonRecebido) {
        try {
            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) return new Resposta(400, "{\"erro\":\"Corpo vazio\"}");

            Usuario dadosNovos = gson.fromJson(jsonRecebido, Usuario.class);
            if (dadosNovos == null || dadosNovos.getCpf() == null || dadosNovos.getSenha() == null || dadosNovos.getSenha().trim().isEmpty()) {
                return new Resposta(400, "{\"erro\":\"CPF e nova senha são obrigatórios\"}");
            } else if (dadosNovos.getSenha().length() < 4) {
                return new Resposta(400, "{\"erro\":\"Senha deve ter no mínimo 4 caracteres\"}");
            }

            Connection conn = Conexao.getConexao();
            String cpfLimpo = dadosNovos.getCpf().replaceAll("[^0-9]", "");

            if (Usuario.buscarPorCPF(conn, cpfLimpo) == null) {
                return new Resposta(404, "{\"erro\":\"CPF não encontrado no sistema\"}");
            } else if (Usuario.mudarSenha(conn, cpfLimpo, Criptografia.hashSenha(dadosNovos.getSenha()))) {
                return new Resposta(200, "{\"mensagem\":\"Senha alterada! Faça login novamente.\"}");
            } else {
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar a senha.\"}");
            }
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao alterar senha. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta processarCadastroInterno(String jsonRecebido, String auth) {
        String email = emailDoToken(auth);
        if (email == null) return new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");

        Connection conn = null;
        try {
            Usuario dados = gson.fromJson(jsonRecebido, Usuario.class);
            if (dados == null || dados.getEmail() == null || dados.getSenha() == null || dados.getTipoUsuario() == null) {
                return new Resposta(400, "{\"erro\":\"Dados inválidos. Email, senha e tipo são obrigatórios.\"}");
            }

            String cpfLimpo = dados.getCpf() != null ? dados.getCpf().replaceAll("[^0-9]", "") : "";
            dados.setCpf(cpfLimpo);

            Map<String, String> erros = dados.validar();
            if (!erros.isEmpty()) return new Resposta(400, gson.toJson(Collections.singletonMap("erros", erros)));

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            Usuario quemCadastra = Usuario.buscarPorEmail(conn, email);
            boolean isAdmin = quemCadastra != null && quemCadastra.getNivelAcesso() == 1;

            String tipo = dados.getTipoUsuario().trim();
            boolean podeCadastrar = isAdmin
                    || usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS");

            if (!podeCadastrar) {
                conn.rollback();
                return new Resposta(403, "{\"erro\":\"Acesso negado. Sem permissão.\"}");
            }

            String senhaBanco = Criptografia.hashSenha(dados.getSenha());
            dados.setSenha(senhaBanco);
            dados.setNivelAcesso(dados.getNivelAcesso() > 0 ? dados.getNivelAcesso() : 2);

            getUsuario().cadastrar(conn, dados);

            if ("colaborador".equalsIgnoreCase(tipo)) {
                Colaborador.cadastrar(conn, dados.getId(), dados.getData());
            } else if ("voluntario".equalsIgnoreCase(tipo)) {
                Voluntario.cadastrar(conn, dados.getId(), dados.getData());
            }

            conn.commit();
            return new Resposta(201, "{\"mensagem\":\"Cadastro realizado!\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao cadastrar usuário. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta listarUsuarios(String auth, String query) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        try {
            Connection conn = Conexao.getConexao();
            String nome = null, emailParam = null, tipo = null;
            if (query != null) {
                for (String p : query.split("&")) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2 && "nome".equalsIgnoreCase(kv[0])) nome = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (kv.length == 2 && "email".equalsIgnoreCase(kv[0])) emailParam = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if (kv.length == 2 && "tipo".equalsIgnoreCase(kv[0])) tipo = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
            List<Usuario> usuarios = getUsuario().filtrar(conn, nome, emailParam, tipo);
            usuarios.forEach(u -> u.setSenha(null));
            return new Resposta(200, gson.toJson(usuarios));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar usuários. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta buscarUsuario(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try {
            Connection conn = Conexao.getConexao();
            Usuario u = Usuario.buscarPorId(conn, id);
            if (u == null) return new Resposta(404, "{\"erro\":\"Usuário não encontrado\"}");

            u.setSenha(null);
            return new Resposta(200, gson.toJson(u));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao buscar usuário. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta atualizarUsuario(String auth, int id, String jsonBody) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            Usuario u = Usuario.buscarPorId(conn, id);
            if (u == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Usuário não encontrado\"}");
            }

            JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
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

            getUsuario().alterar(conn, u);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Usuário atualizado com sucesso\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            String msg = e.getMessage();
            if (msg != null && msg.contains("\"erros\"")) return new Resposta(400, msg);
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar usuário. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta alterarStatusUsuario(String auth, int id, String jsonBody) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
            boolean ativo = body.get("status_ativo").getAsBoolean();

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            Usuario usuario = Usuario.buscarPorId(conn, id);

            if (usuario == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Usuário não encontrado\"}");
            } else if (!ativo && "colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
                if (getUsuario().contarColaboradorAcessoTotalAtivo(conn) <= 1) {
                    conn.rollback();
                    return new Resposta(400, "{\"erro\":\"Não é possível desativar o único colaborador com acesso total\"}");
                }
            }

            return processarAlteracaoStatus(conn, id, usuario, ativo, body);
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao alterar status do usuário. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    private Resposta processarAlteracaoStatus(Connection conn, int id, Usuario usuario, boolean ativo, JsonObject body) throws SQLException {
        getUsuario().alterarStatus(conn, id, ativo);

        String dataStr = body.has("data_desligamento") && !body.get("data_desligamento").isJsonNull()
                ? body.get("data_desligamento").getAsString() : null;

        LocalDate dataDesligamento = null;
        if (!ativo && dataStr != null) {
            dataDesligamento = "SYSDATE".equalsIgnoreCase(dataStr) ? LocalDate.now() : Data.parseFlexivel(dataStr);
        }

        if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario())) {
            Colaborador.atualizarDataDemissao(conn, id, !ativo ? dataDesligamento : null);
        } else if ("voluntario".equalsIgnoreCase(usuario.getTipoUsuario())) {
            Voluntario.atualizarDataDesligamento(conn, id, !ativo ? dataDesligamento : null);
        }
        conn.commit();
        return new Resposta(200, "{\"mensagem\":\"Status atualizado com sucesso\"}");
    }

    public Resposta removerUsuario(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            conn = Conexao.getConexao();
            conn.setAutoCommit(false);
            Usuario usuario = Usuario.buscarPorId(conn, id);

            if (usuario == null) {
                conn.rollback();
                return new Resposta(404, "{\"erro\":\"Usuário não encontrado\"}");
            } else if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
                if (getUsuario().contarColaboradorAcessoTotalAtivo(conn) <= 1) {
                    conn.rollback();
                    return new Resposta(400, "{\"erro\":\"Não é possível remover o único colaborador com acesso total\"}");
                }
            }

            getUsuario().excluir(conn, id);
            conn.commit();
            return new Resposta(200, "{\"mensagem\":\"Usuário removido com sucesso\"}");
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            if (e instanceof SQLException && "23503".equals(((SQLException) e).getSQLState())) {
                return new Resposta(500, "{\"erro\":\"Não é possível remover este usuário pois existem registros vinculados a ele (investimentos, lançamentos etc.). Exclua os vínculos primeiro.\"}");
            }
            return new Resposta(500, "{\"erro\":\"Falha ao remover usuário. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }

    public Resposta listarTodosRecursos(String auth) {
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try {
            Connection conn = Conexao.getConexao();
            List<RecursoSistema> lista = RecursoSistema.listarTodos(conn);
            return new Resposta(200, gson.toJson(lista));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar recursos. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta listarUsuariosPorPermissao(String auth, String permissao) {
        if (emailDoToken(auth) == null) return new Resposta(401, "{\"erro\":\"Acesso negado. Faça login.\"}");
        if (permissao == null || permissao.trim().isEmpty())
            return new Resposta(400, "{\"erro\":\"Parâmetro 'permissao' é obrigatório\"}");
        try {
            Connection conn = Conexao.getConexao();
            List<Usuario> usuarios = RecursoSistema.listarUsuariosPorPermissao(conn, permissao.trim());
            usuarios.forEach(u -> u.setSenha(null));
            return new Resposta(200, gson.toJson(usuarios));
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar usuários por permissão. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta listarPermissoes(String auth, int usuarioId) {
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        try {
            Connection conn = Conexao.getConexao();
            List<RecursoSistema> permissoes = RecursoSistema.listarPorUsuario(conn, usuarioId);
            JsonArray arr = new JsonArray();
            for (RecursoSistema r : permissoes) arr.add(r.getNome());
            JsonObject resp = new JsonObject();
            resp.addProperty("usuarioId", usuarioId);
            resp.add("permissoes", arr);
            return new Resposta(200, resp.toString());
        } catch (Exception e) {
            return new Resposta(500, "{\"erro\":\"Falha ao listar permissões. " + mensagemErroUsuario(e) + "\"}");
        }
    }

    public Resposta atualizarPermissoes(String auth, int usuarioId, String jsonBody) {
        if (!usuarioTemPermissaoDb(auth, "GESTAO_PERMISSOES")) return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        Connection conn = null;
        try {
            JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
            JsonArray arr = body.getAsJsonArray("recursoIds");
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) ids.add(arr.get(i).getAsInt());

            conn = Conexao.getConexao();
            conn.setAutoCommit(false);

            if (RecursoSistema.atualizarPermissoes(conn, usuarioId, ids)) {
                conn.commit();
                return new Resposta(200, "{\"mensagem\":\"Permissões atualizadas\"}");
            } else {
                conn.rollback();
                return new Resposta(500, "{\"erro\":\"Erro ao atualizar permissões\"}");
            }
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return new Resposta(500, "{\"erro\":\"Falha ao atualizar permissões. " + mensagemErroUsuario(e) + "\"}");
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }
}
