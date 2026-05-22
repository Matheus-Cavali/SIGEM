package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;
import org.example.dao.CategoriaDocumentoDao;
import org.example.dao.DocumentoDao;
import org.example.dao.RecursoSistemaDao;
import org.example.dao.UsuarioDao;
import org.example.model.CategoriaDocumento;
import org.example.model.Documento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;
import org.example.util.Data;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DocumentoControl {

    private static DocumentoControl instancia;
    private DocumentoControl() {}
    public static DocumentoControl getInstancia() {
        if (instancia == null) instancia = new DocumentoControl();
        return instancia;
    }

    private String emailDoToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return org.example.util.Token.validarToken(auth.substring(7));
        }
        return null;
    }

    private Usuario usuarioDoToken(Connection conn, String auth) throws SQLException {
        String email = emailDoToken(auth);
        if (email == null) return null;
        return new UsuarioDao().buscarPorEmail(conn, email);
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
        return usuarioTemPermissao(auth, "GESTAO_DOACOES");
    }

    public Resposta criarCategoria(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("nome") || body.get("nome").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Nome da categoria é obrigatório\"}");
        }

        String nome = body.get("nome").getAsString().trim();

        try (Connection conn = Conexao.getConexao()) {
            CategoriaDocumentoDao dao = new CategoriaDocumentoDao();
            if (dao.buscarPorNome(conn, nome) != null) {
                return new Resposta(409, "{\"erro\":\"Já existe uma categoria de documento com este nome\"}");
            }

            CategoriaDocumento categoria = new CategoriaDocumento();
            categoria.setNome(nome);

            int id = dao.inserir(conn, categoria);
            if (id > 0) return new Resposta(201, "{\"mensagem\":\"Categoria de documento cadastrada com sucesso\",\"id\":" + id + "}");
            return new Resposta(500, "{\"erro\":\"Erro ao cadastrar categoria de documento\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao cadastrar categoria de documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao cadastrar categoria de documento\"}");
        }
    }

    public Resposta listarCategorias(String query) {
        String nome = null;
        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2 && "nome".equalsIgnoreCase(kv[0])) {
                    nome = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }

        try (Connection conn = Conexao.getConexao()) {
            List<CategoriaDocumento> lista = new CategoriaDocumentoDao().listar(conn, nome);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                CategoriaDocumento c = lista.get(i);
                json.append("{\"id\":").append(c.getId())
                        .append(",\"nome\":\"").append(escaparJson(c.getNome())).append("\"}");
                if (i < lista.size() - 1) json.append(",");
            }
            json.append("]");
            return new Resposta(200, json.toString());
        } catch (SQLException e) {
            System.err.println("Erro ao listar categorias de documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar categorias de documento\"}");
        }
    }

    public Resposta buscarCategoria(int id) {
        try (Connection conn = Conexao.getConexao()) {
            CategoriaDocumento categoria = new CategoriaDocumentoDao().buscarPorId(conn, id);
            if (categoria == null) return new Resposta(404, "{\"erro\":\"Categoria de documento não encontrada\"}");
            return new Resposta(200, categoriaJson(categoria));
        } catch (SQLException e) {
            System.err.println("Erro ao buscar categoria de documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao buscar categoria de documento\"}");
        }
    }

    public Resposta atualizarCategoria(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);

        if (!body.has("nome") || body.get("nome").getAsString().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Nome da categoria é obrigatório\"}");
        }

        String nome = body.get("nome").getAsString().trim();

        try (Connection conn = Conexao.getConexao()) {
            CategoriaDocumentoDao dao = new CategoriaDocumentoDao();
            CategoriaDocumento categoria = dao.buscarPorId(conn, id);
            if (categoria == null) return new Resposta(404, "{\"erro\":\"Categoria de documento não encontrada\"}");

            CategoriaDocumento existente = dao.buscarPorNome(conn, nome);
            if (existente != null && existente.getId() != id) {
                return new Resposta(409, "{\"erro\":\"Já existe outra categoria de documento com este nome\"}");
            }

            categoria.setNome(nome);
            if (dao.atualizar(conn, categoria)) return new Resposta(200, "{\"mensagem\":\"Categoria de documento atualizada com sucesso\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de documento\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar categoria de documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar categoria de documento\"}");
        }
    }

    public Resposta deletarCategoria(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try (Connection conn = Conexao.getConexao()) {
            CategoriaDocumentoDao dao = new CategoriaDocumentoDao();
            if (dao.buscarPorId(conn, id) == null) return new Resposta(404, "{\"erro\":\"Categoria de documento não encontrada\"}");
            if (dao.deletar(conn, id)) return new Resposta(200, "{\"mensagem\":\"Categoria de documento excluída com sucesso\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao excluir categoria de documento\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao excluir categoria de documento: " + e.getMessage());
            if ("23503".equals(e.getSQLState())) {
                return new Resposta(400, "{\"erro\":\"Não é possível excluir esta categoria porque existem documentos vinculados a ela.\"}");
            }
            return new Resposta(500, "{\"erro\":\"Erro ao excluir categoria de documento\"}");
        }
    }

    public Resposta autorizarUpload(String auth) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }
        return new Resposta(200, "{\"mensagem\":\"Upload autorizado\"}");
    }

    public Resposta cadastrar(String auth, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try (Connection conn = Conexao.getConexao()) {
            Usuario colaborador = usuarioDoToken(conn, auth);
            if (colaborador == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");

            Documento documento = montarDocumento(json, colaborador.getId());
            Resposta validacao = validarDocumento(conn, documento);
            if (validacao != null) return validacao;

            int id = new DocumentoDao().inserir(conn, documento);
            if (id > 0) return new Resposta(201, "{\"mensagem\":\"Documento cadastrado com sucesso\",\"id\":" + id + "}");
            return new Resposta(500, "{\"erro\":\"Erro ao cadastrar documento\"}");
        } catch (Exception e) {
            System.err.println("Erro ao cadastrar documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao cadastrar documento\"}");
        }
    }

    public Resposta listar(String query) {
        String titulo = null;
        Integer categoriaId = null;

        if (query != null) {
            for (String p : query.split("&")) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    String chave = kv[0];
                    String valor = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if ("titulo".equalsIgnoreCase(chave)) titulo = valor;
                    if ("categoriaDocumentoId".equalsIgnoreCase(chave) && !valor.isEmpty()) categoriaId = Integer.parseInt(valor);
                }
            }
        }

        try (Connection conn = Conexao.getConexao()) {
            List<Documento> lista = new DocumentoDao().listar(conn, titulo, categoriaId, null);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < lista.size(); i++) {
                json.append(documentoJson(lista.get(i)));
                if (i < lista.size() - 1) json.append(",");
            }
            json.append("]");
            return new Resposta(200, json.toString());
        } catch (SQLException e) {
            System.err.println("Erro ao listar documentos: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao listar documentos\"}");
        }
    }

    public Resposta buscar(int id) {
        try (Connection conn = Conexao.getConexao()) {
            Documento documento = new DocumentoDao().buscarPorId(conn, id);
            if (documento == null) return new Resposta(404, "{\"erro\":\"Documento não encontrado\"}");
            return new Resposta(200, documentoJson(documento));
        } catch (SQLException e) {
            System.err.println("Erro ao buscar documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao buscar documento\"}");
        }
    }

    public Resposta atualizar(String auth, int id, String json) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try (Connection conn = Conexao.getConexao()) {
            Documento existente = new DocumentoDao().buscarPorId(conn, id);
            if (existente == null) return new Resposta(404, "{\"erro\":\"Documento não encontrado\"}");

            Usuario colaborador = usuarioDoToken(conn, auth);
            if (colaborador == null) return new Resposta(401, "{\"erro\":\"Acesso negado.\"}");

            Documento documento = montarDocumento(json, colaborador.getId());
            documento.setId(id);
            Resposta validacao = validarDocumento(conn, documento);
            if (validacao != null) return validacao;

            if (new DocumentoDao().atualizar(conn, documento)) return new Resposta(200, "{\"mensagem\":\"Documento atualizado\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar documento\"}");
        } catch (Exception e) {
            System.err.println("Erro ao atualizar documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao atualizar documento\"}");
        }
    }

    public Resposta deletar(String auth, int id) {
        if (!usuarioPodeGerenciar(auth)) {
            return new Resposta(403, "{\"erro\":\"Acesso negado.\"}");
        }

        try (Connection conn = Conexao.getConexao()) {
            if (new DocumentoDao().buscarPorId(conn, id) == null) return new Resposta(404, "{\"erro\":\"Documento não encontrado\"}");
            if (new DocumentoDao().deletar(conn, id)) return new Resposta(200, "{\"mensagem\":\"Documento removido\"}");
            return new Resposta(500, "{\"erro\":\"Erro ao remover documento\"}");
        } catch (SQLException e) {
            System.err.println("Erro ao deletar documento: " + e.getMessage());
            return new Resposta(500, "{\"erro\":\"Erro ao remover documento\"}");
        }
    }

    private Documento montarDocumento(String json, int usuarioAutenticadoId) {
        Gson gson = new Gson();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        Documento documento = new Documento();

        if (body.has("titulo") && !body.get("titulo").isJsonNull()) documento.setTitulo(body.get("titulo").getAsString().trim());
        if (body.has("caminhoArquivo") && !body.get("caminhoArquivo").isJsonNull()) documento.setCaminhoArquivo(body.get("caminhoArquivo").getAsString().trim());
        if (body.has("dataDocumento") && !body.get("dataDocumento").isJsonNull()) documento.setDataDocumento(parseData(body.get("dataDocumento").getAsString()));
        if (body.has("categoriaDocumentoId") && !body.get("categoriaDocumentoId").isJsonNull()) documento.setCategoriaDocumentoId(body.get("categoriaDocumentoId").getAsInt());
        documento.setUsuarioTitularId(usuarioAutenticadoId);
        documento.setColaboradorLancouId(usuarioAutenticadoId);

        return documento;
    }

    private Resposta validarDocumento(Connection conn, Documento documento) throws SQLException {
        if (documento.getTitulo() == null || documento.getTitulo().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Título do documento é obrigatório\"}");
        }
        if (documento.getCaminhoArquivo() == null || documento.getCaminhoArquivo().trim().isEmpty()) {
            return new Resposta(400, "{\"erro\":\"Arquivo do documento é obrigatório\"}");
        }
        if (documento.getDataDocumento() == null) {
            return new Resposta(400, "{\"erro\":\"Data do documento é obrigatória\"}");
        }
        if (documento.getCategoriaDocumentoId() != null && new CategoriaDocumentoDao().buscarPorId(conn, documento.getCategoriaDocumentoId()) == null) {
            return new Resposta(404, "{\"erro\":\"Categoria de documento não encontrada\"}");
        }
        if (documento.getUsuarioTitularId() != null && new UsuarioDao().buscarPorId(conn, documento.getUsuarioTitularId()) == null) {
            return new Resposta(404, "{\"erro\":\"Usuário titular não encontrado\"}");
        }
        return null;
    }

    private LocalDate parseData(String dataTexto) {
        if (dataTexto == null || dataTexto.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(dataTexto, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return Data.parseFlexivel(dataTexto);
        }
    }

    private String documentoJson(Documento d) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "{\"id\":" + d.getId()
                + ",\"titulo\":\"" + escaparJson(d.getTitulo()) + "\""
                + ",\"caminhoArquivo\":\"" + escaparJson(d.getCaminhoArquivo()) + "\""
                + ",\"dataDocumento\":\"" + (d.getDataDocumento() != null ? d.getDataDocumento().format(fmt) : "") + "\""
                + ",\"categoriaDocumentoId\":" + (d.getCategoriaDocumentoId() != null ? d.getCategoriaDocumentoId() : "null")
                + ",\"categoriaDocumentoNome\":\"" + escaparJson(d.getCategoriaDocumentoNome()) + "\""
                + ",\"colaboradorLancouId\":" + (d.getColaboradorLancouId() != null ? d.getColaboradorLancouId() : "null")
                + ",\"colaboradorLancouNome\":\"" + escaparJson(d.getColaboradorLancouNome()) + "\""
                + "}";
    }

    private String categoriaJson(CategoriaDocumento c) {
        return "{\"id\":" + c.getId()
                + ",\"nome\":\"" + escaparJson(c.getNome()) + "\"}";
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
