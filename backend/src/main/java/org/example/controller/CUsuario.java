package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.ColaboradorDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.dao.VoluntarioDao;
import org.example.model.RecursoSistema;
import org.example.model.Usuario;
import org.example.util.Criptografia;
import org.example.util.Data;
import org.example.util.Token;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CUsuario implements HttpHandler {

    private static CUsuario instancia;
    private CUsuario() {}
    public static CUsuario getInstancia() {
        if (instancia == null) instancia = new CUsuario();
        return instancia;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // O navegador envia um "OPTIONS" antes do POST para verificar permissões
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(metodo)) {
            if ("/api/login".equals(path)) {
                processarLogin(exchange);
            }
            else if ("/api/cadastrar".equals(path)) {
                processarCadastro(exchange);
            }
            else if ("/api/alterar-Primeira-Senha".equals(path)) {
                processarAlteracaoSenha(exchange);
            }
            else if ("/api/cadastrar-interno".equals(path)) {
                processarCadastroInterno(exchange);
            }
        }
        else if ("GET".equalsIgnoreCase(metodo) && "/api/usuarios".equals(path)) {
            listarUsuarios(exchange);
        }
        else if ("PATCH".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+/status")) {
            alterarStatusUsuario(exchange);
        }
        else if ("GET".equalsIgnoreCase(metodo) && path.matches("/api/recurso/\\d+/permissoes")) {
            listarPermissoes(exchange);
        }
        else if ("PUT".equalsIgnoreCase(metodo) && path.matches("/api/recurso/\\d+/permissoes")) {
            atualizarPermissoes(exchange);
        }
        else if ("GET".equalsIgnoreCase(metodo) && "/api/recurso".equals(path)) {
            listarTodosRecursos(exchange);
        }
        else if ("DELETE".equalsIgnoreCase(metodo) && path.matches("/api/usuarios/\\d+")) {
            removerUsuario(exchange);
        }
    }

    private void processarLogin(HttpExchange exchange) throws IOException {
        try {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);

            if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
                enviarResposta(exchange, "{\"erro\":\"Corpo da requisição vazio\"}", 400);
                return;
            }

            Gson gson = new Gson();
            Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);

            if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getEmail().trim().isEmpty()) {
                enviarResposta(exchange, "{\"erro\":\"Email ou CPF é obrigatório\"}", 400);
                return;
            }

            UsuarioDao dao = new UsuarioDao();
            Usuario usuarioDoBanco;
            String loginInput = usuarioLogin.getEmail().trim();

            if (loginInput.contains("@")) {
                usuarioDoBanco = dao.buscarPorEmail(loginInput);
            } else {
                String cpfLimpo = loginInput.replaceAll("[^0-9]", "");
                if (cpfLimpo.length() == 11) {
                    usuarioDoBanco = dao.buscarPorCPF(cpfLimpo);
                } else {
                    usuarioDoBanco = null;
                }
            }

            if (usuarioDoBanco == null || !Criptografia.verificarSenha(usuarioLogin.getSenha(), usuarioDoBanco.getSenha())) {
                String campo = loginInput.contains("@") ? "E-mail" : "CPF";
                enviarResposta(exchange, "{\"erro\":\"" + campo + " ou senha incorretos\"}", 401);
                return;
            }

            if (!usuarioDoBanco.isStatusAtivo()) {
                enviarResposta(exchange, "{\"erro\":\"Usuário desativado. Contate um administrador.\"}", 403);
                return;
            }

            if ("voluntario".equalsIgnoreCase(usuarioDoBanco.getTipoUsuario())) {
                enviarResposta(exchange, "{\"erro\":\"Voluntários não podem acessar o sistema. Apenas colaboradores.\"}", 403);
                return;
            }

            if (usuarioDoBanco.isPrimeiroAcesso()) {
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "TROCA_OBRIGATORIA");
                resp.addProperty("mensagem", "Primeiro acesso detectado. Altere a sua senha.");
                resp.addProperty("cpf", usuarioDoBanco.getCpf());
                enviarResposta(exchange, resp.toString(), 200);
                return;
            }

            RecursoSistemaDao recDao = new RecursoSistemaDao();
            List<RecursoSistema> permissoes;
            if (usuarioDoBanco.getNivelAcesso() == 1) {
                permissoes = recDao.listarTodos();
            } else {
                permissoes = recDao.listarPorUsuario(usuarioDoBanco.getId());
            }

            JsonArray permArray = new JsonArray();
            for (RecursoSistema r : permissoes) {
                permArray.add(r.getNome());
            }

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
            enviarResposta(exchange, resp.toString(), 200);
        } catch (Exception e) {
            System.err.println("ERRO no processarLogin: " + e.getMessage());
            e.printStackTrace();
            enviarResposta(exchange, "{\"erro\":\"Erro interno: " + e.getMessage().replace("\"", "'") + "\"}", 500);
        }
    }

    private void processarCadastro(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);

        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            enviarResposta(exchange, "{\"erro\":\"Corpo da requisição vazio\"}", 400);
            return;
        }

        Gson gson = new Gson();
        Usuario usuarioLogin = gson.fromJson(jsonRecebido, Usuario.class);
        if (usuarioLogin == null || usuarioLogin.getEmail() == null || usuarioLogin.getSenha() == null || usuarioLogin.getSenha().trim().isEmpty()) {
            enviarResposta(exchange, "{\"erro\":\"Dados inválidos. Email e senha são obrigatórios.\"}", 400);
            return;
        }

        if (usuarioLogin.getSenha().length() < 4) {
            enviarResposta(exchange, "{\"erro\":\"Senha deve ter no mínimo 4 caracteres\"}", 400);
            return;
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

        if (resultado == 0) {
            enviarResposta(exchange, "{\"mensagem\":\"Cadastro realizado!\"}", 201);
        } else if (resultado == 1) {
            enviarResposta(exchange, "{\"erro\":\"Email já cadastrado no sistema\"}", 409);
        } else if (resultado == 2) {
            enviarResposta(exchange, "{\"erro\":\"CPF já cadastrado no sistema\"}", 409);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro interno ao cadastrar\"}", 500);
        }
    }

    private String emailDoToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return Token.validarToken(auth.substring(7));
    }

    private boolean usuarioTemPermissaoDb(HttpExchange exchange, String recursoNome) {
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

    private void processarCadastroInterno(HttpExchange exchange) throws IOException {
        String email = emailDoToken(exchange);
        if (email == null) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Faça login.\"}", 401);
            return;
        }

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);

        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            enviarResposta(exchange, "{\"erro\":\"Corpo da requisição vazio\"}", 400);
            return;
        }

        Gson gson = new Gson();
        Usuario dados = gson.fromJson(jsonRecebido, Usuario.class);
        if (dados == null || dados.getEmail() == null || dados.getSenha() == null || dados.getTipoUsuario() == null) {
            enviarResposta(exchange, "{\"erro\":\"Dados inválidos. Email, senha e tipo são obrigatórios.\"}", 400);
            return;
        }

        if (dados.getSenha().length() < 4) {
            enviarResposta(exchange, "{\"erro\":\"Senha deve ter no mínimo 4 caracteres\"}", 400);
            return;
        }

        // Verifica permissão
        String tipo = dados.getTipoUsuario().trim();
        boolean isAdmin = false;
        UsuarioDao uDao = new UsuarioDao();
        Usuario quemCadastra = uDao.buscarPorEmail(email);
        if (quemCadastra != null && quemCadastra.getNivelAcesso() == 1) isAdmin = true;

        if (!isAdmin) {
            if ("colaborador".equalsIgnoreCase(tipo) && !usuarioTemPermissaoDb(exchange, "GESTAO_USUARIOS") && !usuarioTemPermissaoDb(exchange, "GESTAO_COLABORADORES")) {
                enviarResposta(exchange, "{\"erro\":\"Acesso negado. Você não pode cadastrar colaboradores.\"}", 403);
                return;
            }
            if ("voluntario".equalsIgnoreCase(tipo) && !usuarioTemPermissaoDb(exchange, "GESTAO_USUARIOS") && !usuarioTemPermissaoDb(exchange, "GESTAO_VOLUNTARIOS")) {
                enviarResposta(exchange, "{\"erro\":\"Acesso negado. Você não pode cadastrar voluntários.\"}", 403);
                return;
            }
        }

        String senhaBanco = Criptografia.hashSenha(dados.getSenha());
        String cpfLimpo = dados.getCpf() != null ? dados.getCpf().replaceAll("[^0-9]", "") : "";

        int resultado = uDao.cadastrarUsuario(
            dados.getNome(), dados.getEmail(), senhaBanco,
            cpfLimpo, dados.getNivelAcesso(),
            tipo, dados.getData()
        );

        if (resultado == 0) {
            enviarResposta(exchange, "{\"mensagem\":\"Cadastro realizado!\"}", 201);
        } else if (resultado == 1) {
            enviarResposta(exchange, "{\"erro\":\"Email já cadastrado no sistema\"}", 409);
        } else if (resultado == 2) {
            enviarResposta(exchange, "{\"erro\":\"CPF já cadastrado no sistema\"}", 409);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro interno ao cadastrar\"}", 500);
        }
    }

    private void processarAlteracaoSenha(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);

        if (jsonRecebido == null || jsonRecebido.trim().isEmpty()) {
            enviarResposta(exchange, "{\"erro\":\"Corpo da requisição vazio\"}", 400);
            return;
        }

        Gson gson = new Gson();
        Usuario dadosNovos = gson.fromJson(jsonRecebido, Usuario.class);

        if (dadosNovos == null || dadosNovos.getCpf() == null || dadosNovos.getSenha() == null || dadosNovos.getSenha().trim().isEmpty()) {
            enviarResposta(exchange, "{\"erro\":\"CPF e nova senha são obrigatórios\"}", 400);
            return;
        }

        if (dadosNovos.getSenha().length() < 4) {
            enviarResposta(exchange, "{\"erro\":\"Senha deve ter no mínimo 4 caracteres\"}", 400);
            return;
        }

        UsuarioDao dao = new UsuarioDao();
        String cpfLimpo = dadosNovos.getCpf().replaceAll("[^0-9]", "");

        if (dao.buscarPorCPF(cpfLimpo) == null) {
            enviarResposta(exchange, "{\"erro\":\"CPF não encontrado no sistema\"}", 404);
            return;
        }

        String senhaNova = Criptografia.hashSenha(dadosNovos.getSenha());
        boolean sucesso = dao.mudarSenha(cpfLimpo, senhaNova);
        if (sucesso) {
            enviarResposta(exchange, "{\"mensagem\":\"Senha alterada! Faça login novamente.\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar a senha.\"}", 500);
        }
    }

    private void listarUsuarios(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String nome = null, email = null, tipo = null;
        if (query != null) {
            String[] params = query.split("&");
            for (String p : params) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    if ("nome".equalsIgnoreCase(kv[0])) nome = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if ("email".equalsIgnoreCase(kv[0])) email = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if ("tipo".equalsIgnoreCase(kv[0])) tipo = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        UsuarioDao dao = new UsuarioDao();
        List<Usuario> usuarios = dao.listarTodos(nome, email, tipo);

        Gson gson = new Gson();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < usuarios.size(); i++) {
            Usuario u = usuarios.get(i);
            json.append("{");
            json.append("\"id\":").append(u.getId()).append(",");
            json.append("\"nome\":\"").append(escaparJson(u.getNome())).append("\",");
            json.append("\"email\":\"").append(escaparJson(u.getEmail())).append("\",");
            json.append("\"cpf\":\"").append(escaparJson(u.getCpf())).append("\",");
            json.append("\"nivelAcesso\":").append(u.getNivelAcesso()).append(",");
            json.append("\"statusAtivo\":").append(u.isStatusAtivo()).append(",");
            json.append("\"tipoUsuario\":\"").append(escaparJson(u.getTipoUsuario())).append("\",");
            json.append("\"primeiroAcesso\":").append(u.isPrimeiroAcesso());
            json.append("}");
            if (i < usuarios.size() - 1) json.append(",");
        }
        json.append("]");
        enviarResposta(exchange, json.toString(), 200);
    }

    private boolean usuarioPodeGerenciar(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        String token = auth.substring(7);
        int nivel = Token.extrairNivelAcesso(token);
        return nivel == 1;
    }

    private void alterarStatusUsuario(HttpExchange exchange) throws IOException {
        if (!usuarioPodeGerenciar(exchange)) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Apenas administradores podem alterar status de usuários.\"}", 403);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] partes = path.split("/");
        int id = Integer.parseInt(partes[3]);

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String jsonRecebido = new String(bytes, StandardCharsets.UTF_8);

        Gson gson = new Gson();
        JsonObject body = gson.fromJson(jsonRecebido, JsonObject.class);
        boolean ativo = body.get("status_ativo").getAsBoolean();

        UsuarioDao dao = new UsuarioDao();
        Usuario usuario = dao.buscarPorId(id);

        if (usuario == null) {
            enviarResposta(exchange, "{\"erro\":\"Usuário não encontrado\"}", 404);
            return;
        }

        if (!ativo && "colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
            int totalAtivos = dao.contarColaboradorAcessoTotalAtivo();
            if (totalAtivos <= 1) {
                enviarResposta(exchange, "{\"erro\":\"Não é possível desativar o único colaborador com acesso total\"}", 400);
                return;
            }
        }

        if (dao.alterarStatus(id, ativo)) {
            // Atualiza data de desligamento / limpa ao reativar
            String tipo = usuario.getTipoUsuario();
            String dataStr = body.has("data_desligamento") && !body.get("data_desligamento").isJsonNull()
                ? body.get("data_desligamento").getAsString() : null;

            LocalDate dataDesligamento = null;
            if (!ativo && dataStr != null) {
                if ("SYSDATE".equalsIgnoreCase(dataStr)) {
                    dataDesligamento = LocalDate.now();
                } else {
                    dataDesligamento = Data.parseFlexivel(dataStr);
                }
            }

            if ("colaborador".equalsIgnoreCase(tipo)) {
                ColaboradorDao cDao = new ColaboradorDao();
                cDao.atualizarDataDemissao(id, !ativo ? dataDesligamento : null);
            } else if ("voluntario".equalsIgnoreCase(tipo)) {
                VoluntarioDao vDao = new VoluntarioDao();
                vDao.atualizarDataDesligamento(id, !ativo ? dataDesligamento : null);
            }

            enviarResposta(exchange, "{\"mensagem\":\"Status atualizado com sucesso\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar status\"}", 500);
        }
    }

    private void removerUsuario(HttpExchange exchange) throws IOException {
        if (!usuarioPodeGerenciar(exchange)) {
            enviarResposta(exchange, "{\"erro\":\"Acesso negado. Apenas administradores podem remover usuários.\"}", 403);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String[] partes = path.split("/");
        int id = Integer.parseInt(partes[3]);

        UsuarioDao dao = new UsuarioDao();
        Usuario usuario = dao.buscarPorId(id);

        if (usuario == null) {
            enviarResposta(exchange, "{\"erro\":\"Usuário não encontrado\"}", 404);
            return;
        }

        if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
            int totalAtivos = dao.contarColaboradorAcessoTotalAtivo();
            if (totalAtivos <= 1) {
                enviarResposta(exchange, "{\"erro\":\"Não é possível remover o único colaborador com acesso total\"}", 400);
                return;
            }
        }

        if (dao.deletar(id)) {
            enviarResposta(exchange, "{\"mensagem\":\"Usuário removido com sucesso\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao remover usuário\"}", 500);
        }
    }

    private void listarTodosRecursos(HttpExchange exchange) throws IOException {
        RecursoSistemaDao dao = new RecursoSistemaDao();
        List<RecursoSistema> lista = dao.listarTodos();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            RecursoSistema r = lista.get(i);
            json.append("{");
            json.append("\"id\":").append(r.getId()).append(",");
            json.append("\"nome\":\"").append(escaparJson(r.getNome())).append("\",");
            json.append("\"descricao\":\"").append(escaparJson(r.getDescricao())).append("\"");
            json.append("}");
            if (i < lista.size() - 1) json.append(",");
        }
        json.append("]");
        enviarResposta(exchange, json.toString(), 200);
    }

    private void listarPermissoes(HttpExchange exchange) throws IOException {
        String[] partes = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(partes[3]);

        RecursoSistemaDao dao = new RecursoSistemaDao();
        List<RecursoSistema> permissoes = dao.listarPorUsuario(usuarioId);

        JsonArray arr = new JsonArray();
        for (RecursoSistema r : permissoes) {
            arr.add(r.getNome());
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("usuarioId", usuarioId);
        resp.add("permissoes", arr);
        enviarResposta(exchange, resp.toString(), 200);
    }

    private void atualizarPermissoes(HttpExchange exchange) throws IOException {
        String[] partes = exchange.getRequestURI().getPath().split("/");
        int usuarioId = Integer.parseInt(partes[3]);

        byte[] bytes = exchange.getRequestBody().readAllBytes();
        String json = new String(bytes, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        JsonArray arr = body.getAsJsonArray("recursoIds");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            ids.add(arr.get(i).getAsInt());
        }

        RecursoSistemaDao dao = new RecursoSistemaDao();
        if (dao.atualizarPermissoes(usuarioId, ids)) {
            enviarResposta(exchange, "{\"mensagem\":\"Permissões atualizadas\"}", 200);
        } else {
            enviarResposta(exchange, "{\"erro\":\"Erro ao atualizar permissões\"}", 500);
        }
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