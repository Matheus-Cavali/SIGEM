package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.dao.VoluntarioDao;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.util.Criptografia;
import org.example.util.Data;
import org.example.util.Token;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CUsuario {

    private static CUsuario instancia;
    private CUsuario() {}
    public static CUsuario getInstancia() {
        if (instancia == null) instancia = new CUsuario();
        return instancia;
    }

    private String emailDoToken(String auth) {
        String resultado = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            resultado = Token.validarToken(auth.substring(7));
        }
        return resultado;
    }

    private boolean usuarioTemPermissaoDb(String auth, String recursoNome) {
        boolean permitido = false;
        String email = emailDoToken(auth);
        if (email != null) {
            UsuarioDao uDao = new UsuarioDao();
            Usuario u = uDao.buscarPorEmail(email);
            if (u != null) {
                if (u.getNivelAcesso() == 1) {
                    permitido = true;
                } else {
                    RecursoSistemaDao rDao = new RecursoSistemaDao();
                    List<RecursoSistema> permissoes = rDao.listarPorUsuario(u.getId());
                    for (RecursoSistema r : permissoes) {
                        if (r.getNome().equals(recursoNome)) {
                            permitido = true;
                        }
                    }
                }
            }
        }
        return permitido;
    }

    private boolean usuarioPodeGerenciar(String auth) {
        boolean resultado = false;
        String email = emailDoToken(auth);
        if (email != null) {
            UsuarioDao uDao = new UsuarioDao();
            Usuario u = uDao.buscarPorEmail(email);
            if (u != null) {
                resultado = (u.getNivelAcesso() == 1);
            }
        }
        return resultado;
    }

    public Resposta processarLogin(String jsonRecebido) {
        try {
            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
                return new Resposta(400, "{\"erro\":\"Corpo da requisi\u00e7\u00e3o vazio\"}");
            }
            Gson gson = new Gson();
            Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);

            if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getEmail().trim().isEmpty()) {
                return new Resposta(400, "{\"erro\":\"Email ou CPF \u00e9 obrigat\u00f3rio\"}");
            }

            UsuarioDao dao = new UsuarioDao();
            Usuario usuarioDoBanco;
            String loginInput = usuarioLogin.getEmail().trim();

            if (loginInput.contains("@")) {
                usuarioDoBanco = dao.buscarPorEmail(loginInput);
            } else {
                String cpfLimpo = loginInput.replaceAll("[^0-9]", "");
                usuarioDoBanco = cpfLimpo.length() == 11 ? dao.buscarPorCPF(cpfLimpo) : null;
            }

            if (usuarioDoBanco == null || !Criptografia.verificarSenha(usuarioLogin.getSenha(), usuarioDoBanco.getSenha())) {
                String campo = loginInput.contains("@") ? "E-mail" : "CPF";
                return new Resposta(401, "{\"erro\":\"" + campo + " ou senha incorretos\"}");
            }
            if (!usuarioDoBanco.isStatusAtivo()) {
                return new Resposta(403, "{\"erro\":\"Usu\u00e1rio desativado. Contate um administrador.\"}");
            }
            if ("voluntario".equalsIgnoreCase(usuarioDoBanco.getTipoUsuario())) {
                return new Resposta(403, "{\"erro\":\"Volunt\u00e1rios n\u00e3o podem acessar o sistema. Apenas colaboradores.\"}");
            }
            if (usuarioDoBanco.isPrimeiroAcesso()) {
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "TROCA_OBRIGATORIA");
                resp.addProperty("mensagem", "Primeiro acesso detectado. Altere a sua senha.");
                resp.addProperty("cpf", usuarioDoBanco.getCpf());
                return new Resposta(200, resp.toString());
            }

            RecursoSistemaDao recDao = new RecursoSistemaDao();
            List<RecursoSistema> permissoes = usuarioDoBanco.getNivelAcesso() == 1
                ? recDao.listarTodos() : recDao.listarPorUsuario(usuarioDoBanco.getId());

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
        } catch (Exception e) {
            System.err.println("ERRO no processarLogin: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro interno: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    public Resposta processarCadastro(String jsonRecebido) {
        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Corpo da requisi\u00e7\u00e3o vazio\"}");
        }
        Gson gson = new Gson();
        Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);
        if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getSenha() == null || usuarioLogin.getSenha().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Dados inv\u00e1lidos. Email e senha s\u00e3o obrigat\u00f3rios.\"}");
        }
        if (usuarioLogin.getSenha().length() < 4) {
            return new Resposta(400, "{\"erro\":\"Senha deve ter no m\u00ednimo 4 caracteres\"}");
        }

        String senhaBanco = Criptografia.hashSenha(usuarioLogin.getSenha());
        String cpfLimpo = usuarioLogin.getCpf() != null ? usuarioLogin.getCpf().replaceAll("[^0-9]", "") : "";
        usuarioLogin.setCpf(cpfLimpo);

        UsuarioDao dao = new UsuarioDao();
        int resultado = dao.cadastrarUsuario(
            usuarioLogin.getNome(), usuarioLogin.getEmail(), senhaBanco,
            usuarioLogin.getCpf(), usuarioLogin.getNivelAcesso(),
            usuarioLogin.getTipoUsuario(), usuarioLogin.getData()
        );

        if (resultado == 0) return new Resposta(201, "{\"mensagem\":\"Cadastro realizado!\"}");
        if (resultado == 1) return new Resposta(409, "{\"erro\":\"Email j\u00e1 cadastrado no sistema\"}");
        if (resultado == 2) return new Resposta(409, "{\"erro\":\"CPF j\u00e1 cadastrado no sistema\"}");
        return new Resposta(500, "{\"erro\":\"Erro interno ao cadastrar\"}");
    }

    public Resposta processarAlteracaoSenha(String jsonRecebido) {
        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Corpo da requisi\u00e7\u00e3o vazio\"}");
        }
        Gson gson = new Gson();
        Usuario dadosNovos = gson.fromJson(jsonRecebido, Usuario.class);
        if (dadosNovos == null || dadosNovos.getCpf() == null || dadosNovos.getSenha() == null || dadosNovos.getSenha().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"CPF e nova senha s\u00e3o obrigat\u00f3rios\"}");
        }
        if (dadosNovos.getSenha().length() < 4) {
            return new Resposta(400, "{\"erro\":\"Senha deve ter no m\u00ednimo 4 caracteres\"}");
        }

        UsuarioDao dao = new UsuarioDao();
        String cpfLimpo = dadosNovos.getCpf().replaceAll("[^0-9]", "");

        if (dao.buscarPorCPF(cpfLimpo) == null) {
            return new Resposta(404, "{\"erro\":\"CPF n\u00e3o encontrado no sistema\"}");
        }

        boolean sucesso = dao.mudarSenha(cpfLimpo, Criptografia.hashSenha(dadosNovos.getSenha()));
        if (sucesso) {
            return new Resposta(200, "{\"mensagem\":\"Senha alterada! Fa\u00e7a login novamente.\"}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar a senha.\"}");
    }

    public Resposta processarCadastroInterno(String jsonRecebido, String auth) {
        String email = emailDoToken(auth);
        if (email == null) {
            return new Resposta(401, "{\"erro\":\"Acesso negado. Fa\u00e7a login.\"}");
        }
        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Corpo da requisi\u00e7\u00e3o vazio\"}");
        }
        Gson gson = new Gson();
        Usuario dados = gson.fromJson(jsonRecebido, Usuario.class);
        if (dados == null || dados.getEmail() == null || dados.getSenha() == null || dados.getTipoUsuario() == null) {
            return new Resposta(400, "{\"erro\":\"Dados inv\u00e1lidos. Email, senha e tipo s\u00e3o obrigat\u00f3rios.\"}");
        }
        if (dados.getSenha().length() < 4) {
            return new Resposta(400, "{\"erro\":\"Senha deve ter no m\u00ednimo 4 caracteres\"}");
        }

        String tipo = dados.getTipoUsuario().trim();
        UsuarioDao uDao = new UsuarioDao();
        Usuario quemCadastra = uDao.buscarPorEmail(email);
        boolean isAdmin = quemCadastra != null && quemCadastra.getNivelAcesso() == 1;

        if (!isAdmin && "colaborador".equalsIgnoreCase(tipo) && !usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS") && !usuarioTemPermissaoDb(auth, "GESTAO_COLABORADORES")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado. Voc\u00ea n\u00e3o pode cadastrar colaboradores.\"}");
        }
        if (!isAdmin && "voluntario".equalsIgnoreCase(tipo) && !usuarioTemPermissaoDb(auth, "GESTAO_USUARIOS") && !usuarioTemPermissaoDb(auth, "GESTAO_VOLUNTARIOS")) {
            return new Resposta(403, "{\"erro\":\"Acesso negado. Voc\u00ea n\u00e3o pode cadastrar volunt\u00e1rios.\"}");
        }

        String senhaBanco = Criptografia.hashSenha(dados.getSenha());
        String cpfLimpo = dados.getCpf() != null ? dados.getCpf().replaceAll("[^0-9]", "") : "";
        int resultado = uDao.cadastrarUsuario(
            dados.getNome(), dados.getEmail(), senhaBanco, cpfLimpo,
            dados.getNivelAcesso(), tipo, dados.getData()
        );

        if (resultado == 0) return new Resposta(201, "{\"mensagem\":\"Cadastro realizado!\"}");
        if (resultado == 1) return new Resposta(409, "{\"erro\":\"Email j\u00e1 cadastrado no sistema\"}");
        if (resultado == 2) return new Resposta(409, "{\"erro\":\"CPF j\u00e1 cadastrado no sistema\"}");
        return new Resposta(500, "{\"erro\":\"Erro interno ao cadastrar\"}");
    }

    public Resposta listarUsuarios(String query) {
        String nome = null, email = null, tipo = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if ("email".equalsIgnoreCase(kv[0])) email = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if ("tipo".equalsIgnoreCase(kv[0])) tipo = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }

        UsuarioDao dao = new UsuarioDao();
        List<Usuario> usuarios = dao.listarTodos(nome, email, tipo);

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
        return new Resposta(200, json.toString());
    }

    public Resposta buscarUsuario(int id) {
        UsuarioDao dao = new UsuarioDao();
        Usuario u = dao.buscarPorId(id);
        if (u == null) {
            return new Resposta(404, "{\"erro\":\"Usu\u00e1rio n\u00e3o encontrado\"}");
        }
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
        return new Resposta(200, json.toString());
    }

    public Resposta atualizarUsuario(String auth, int id, String jsonBody) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonBody, JsonObject.class);

        UsuarioDao dao = new UsuarioDao();
        Usuario u = dao.buscarPorId(id);
        if (u == null) {
            return new Resposta(404, "{\"erro\":\"Usu\u00e1rio n\u00e3o encontrado\"}");
        }

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

        if (dao.atualizar(u)) {
            return new Resposta(200, "{\"mensagem\":\"Usu\u00e1rio atualizado com sucesso\"}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar usu\u00e1rio\"}");
    }

    public Resposta alterarStatusUsuario(String auth, int id, String jsonBody) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
        boolean ativo = body.get("status_ativo").getAsBoolean();

        UsuarioDao dao = new UsuarioDao();
        Usuario usuario = dao.buscarPorId(id);
        if (usuario == null) {
            return new Resposta(404, "{\"erro\":\"Usu\u00e1rio n\u00e3o encontrado\"}");
        }

        if (!ativo && "colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
            int totalAtivos = dao.contarColaboradorAcessoTotalAtivo();
            if (totalAtivos <= 1) {
                return new Resposta(400, "{\"erro\":\"N\u00e3o \u00e9 poss\u00edvel desativar o \u00fanico colaborador com acesso total\"}");
            }
        }

        if (dao.alterarStatus(id, ativo)) {
            String tipo = usuario.getTipoUsuario();
            String dataStr = body.has("data_desligamento") && !body.get("data_desligamento").isJsonNull()
                ? body.get("data_desligamento").getAsString() : null;

            LocalDate dataDesligamento = null;
            if (!ativo && dataStr != null) {
                dataDesligamento = "SYSDATE".equalsIgnoreCase(dataStr) ? LocalDate.now() : Data.parseFlexivel(dataStr);
            }

            if ("colaborador".equalsIgnoreCase(tipo)) {
                new ColaboradorDao().atualizarDataDemissao(id, !ativo ? dataDesligamento : null);
            } else if ("voluntario".equalsIgnoreCase(tipo)) {
                new VoluntarioDao().atualizarDataDesligamento(id, !ativo ? dataDesligamento : null);
            }
            return new Resposta(200, "{\"mensagem\":\"Status atualizado com sucesso\"}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar status\"}");
    }

    public Resposta removerUsuario(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        UsuarioDao dao = new UsuarioDao();
        Usuario usuario = dao.buscarPorId(id);
        if (usuario == null) {
            return new Resposta(404, "{\"erro\":\"Usu\u00e1rio n\u00e3o encontrado\"}");
        }
        if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
            if (dao.contarColaboradorAcessoTotalAtivo() <= 1) {
                return new Resposta(400, "{\"erro\":\"N\u00e3o \u00e9 poss\u00edvel remover o \u00fanico colaborador com acesso total\"}");
            }
        }
        if (dao.deletar(id)) {
            return new Resposta(200, "{\"mensagem\":\"Usu\u00e1rio removido com sucesso\"}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao remover usu\u00e1rio\"}");
    }

    public Resposta listarTodosRecursos() {
        RecursoSistemaDao dao = new RecursoSistemaDao();
        List<RecursoSistema> lista = dao.listarTodos();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            RecursoSistema r = lista.get(i);
            json.append("{\"id\":").append(r.getId()).append(",");
            json.append("\"nome\":\"").append(escaparJson(r.getNome())).append("\",");
            json.append("\"descricao\":\"").append(escaparJson(r.getDescricao())).append("\"}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        return new Resposta(200, json.toString());
    }

    public Resposta listarPermissoes(int usuarioId) {
        RecursoSistemaDao dao = new RecursoSistemaDao();
        List<RecursoSistema> permissoes = dao.listarPorUsuario(usuarioId);
        JsonArray arr = new JsonArray();
        for (RecursoSistema r : permissoes) arr.add(r.getNome());
        JsonObject resp = new JsonObject();
        resp.addProperty("usuarioId", usuarioId);
        resp.add("permissoes", arr);
        return new Resposta(200, resp.toString());
    }

    public Resposta atualizarPermissoes(int usuarioId, String jsonBody) {
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonBody, JsonObject.class);
        JsonArray arr = body.getAsJsonArray("recursoIds");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) ids.add(arr.get(i).getAsInt());

        RecursoSistemaDao dao = new RecursoSistemaDao();
        if (dao.atualizarPermissoes(usuarioId, ids)) {
            return new Resposta(200, "{\"mensagem\":\"Permiss\u00f5es atualizadas\"}");
        }
        return new Resposta(500, "{\"erro\":\"Erro ao atualizar permiss\u00f5es\"}");
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

