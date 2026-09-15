package org.example.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.conexao.Conexao;

import org.example.model.CategoriaEvento;
import org.example.model.RecursoSistema;
import org.example.model.Resposta;
import org.example.model.Usuario;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaEventoControl {

    private static CategoriaEventoControl instancia;

    private CategoriaEventoControl() {}

    public static synchronized CategoriaEventoControl getInstancia() {
        if (instancia == null) instancia = new CategoriaEventoControl();
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

            try (Connection conn = Conexao.getConexao()) {

                Usuario u = Usuario.buscarPorEmail(conn, email);

                if (u != null) {

                    if (u.getNivelAcesso() == 1) {

                        permitido = true;

                    } else {

                        for (RecursoSistema r : RecursoSistema.listarPorUsuario(conn, u.getId())) {

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

    public Resposta cadastrar(String auth, String json) {

        Resposta result;

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {

            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        } else {

            Connection conn = null;

            try {

                Gson gson = new Gson();

                CategoriaEvento categoria = gson.fromJson(json, CategoriaEvento.class);

                Map<String, String> erros = categoria.validar();

                if (!erros.isEmpty()) {

                    result = new Resposta(
                            400,
                            gson.toJson(Collections.singletonMap("erros", erros))
                    );

                } else {

                    conn = Conexao.getConexao();

                    conn.setAutoCommit(false);

                    CategoriaEvento existente =
                            CategoriaEvento.buscarPorNome(conn, categoria.getNome());

                    if (existente != null) {

                        conn.rollback();

                        Map<String, String> err = new LinkedHashMap<>();

                        err.put("nome", "Categoria de evento jÃ¡ cadastrada");

                        result = new Resposta(
                                409,
                                gson.toJson(Collections.singletonMap("erros", err))
                        );

                    } else {

                        int id = CategoriaEvento.salvar(conn, categoria);

                        if (id > 0) {

                            conn.commit();

                            result = new Resposta(
                                    201,
                                    "{\"mensagem\":\"Categoria cadastrada com sucesso\",\"id\":" + id + "}"
                            );

                        } else {

                            conn.rollback();

                            result = new Resposta(
                                    500,
                                    "{\"erro\":\"Erro ao cadastrar categoria\"}"
                            );
                        }
                    }
                }

            } catch (Exception e) {

                if (conn != null) {

                    try {

                        conn.rollback();

                    } catch (SQLException ex) {

                        System.err.println("Erro no rollback: " + ex.getMessage());
                    }
                }

                System.err.println("Erro ao cadastrar categoria: " + e.getMessage());

                result = new Resposta(
                        500,
                        "{\"erro\":\"Falha ao cadastrar categoria.\"}"
                );

            } finally {

                if (conn != null) {

                    try {

                        conn.close();

                    } catch (SQLException e) {

                        System.err.println("Erro ao fechar conexao: " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    public Resposta listar(String auth, String query) {

        Resposta result;

        if (emailDoToken(auth) == null) {

            result = new Resposta(
                    401,
                    "{\"erro\":\"Acesso negado. FaÃ§a login.\"}"
            );

        } else {

            try {

                String nome = null;

                if (query != null) {

                    for (String param : query.split("&")) {

                        String[] pair = param.split("=", 2);

                        if (pair.length == 2) {

                            if ("nome".equalsIgnoreCase(pair[0])) {

                                nome = URLDecoder.decode(
                                        pair[1],
                                        StandardCharsets.UTF_8
                                );
                            }
                        }
                    }
                }

                try (Connection conn = Conexao.getConexao()) {

                    List<CategoriaEvento> lista =
                            CategoriaEvento.listar(conn, nome);

                    StringBuilder json = new StringBuilder("[");

                    for (int i = 0; i < lista.size(); i++) {

                        CategoriaEvento categoria = lista.get(i);

                        json.append("{");
                        json.append("\"id\":").append(categoria.getId()).append(",");
                        json.append("\"nome\":\"")
                                .append(escaparJson(categoria.getNome()))
                                .append("\"");
                        json.append("}");

                        if (i < lista.size() - 1) {
                            json.append(",");
                        }
                    }

                    json.append("]");

                    result = new Resposta(200, json.toString());
                }

            } catch (Exception e) {

                System.err.println("Erro ao listar categorias: " + e.getMessage());

                result = new Resposta(
                        500,
                        "{\"erro\":\"Erro ao listar categorias.\"}"
                );
            }
        }

        return result;
    }

    public Resposta atualizar(String auth, int id, String json) {

        Resposta result;

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {

            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        } else {

            Connection conn = null;

            try {

                Gson gson = new Gson();

                CategoriaEvento categoria =
                        gson.fromJson(json, CategoriaEvento.class);

                categoria.setId(id);

                Map<String, String> erros = categoria.validar();

                if (!erros.isEmpty()) {

                    result = new Resposta(
                            400,
                            gson.toJson(Collections.singletonMap("erros", erros))
                    );

                } else {

                    conn = Conexao.getConexao();

                    conn.setAutoCommit(false);

                    CategoriaEvento existente =
                            CategoriaEvento.buscarPorId(conn, id);

                    if (existente == null) {

                        conn.rollback();

                        result = new Resposta(
                                404,
                                "{\"erro\":\"Categoria nÃ£o encontrada\"}"
                        );

                    } else {

                        CategoriaEvento duplicada =
                                CategoriaEvento.buscarPorNome(
                                        conn,
                                        categoria.getNome()
                                );

                        if (
                                duplicada != null &&
                                        !duplicada.getId().equals(id)
                        ) {

                            conn.rollback();

                            Map<String, String> err = new LinkedHashMap<>();

                            err.put("nome", "Categoria jÃ¡ cadastrada");

                            result = new Resposta(
                                    409,
                                    gson.toJson(Collections.singletonMap("erros", err))
                            );

                        } else {

                            if (CategoriaEvento.atualizar(conn, categoria)) {

                                conn.commit();

                                result = new Resposta(
                                        200,
                                        "{\"mensagem\":\"Categoria atualizada com sucesso\"}"
                                );

                            } else {

                                conn.rollback();

                                result = new Resposta(
                                        500,
                                        "{\"erro\":\"Erro ao atualizar categoria\"}"
                                );
                            }
                        }
                    }
                }

            } catch (Exception e) {

                if (conn != null) {

                    try {

                        conn.rollback();

                    } catch (SQLException ex) {

                        System.err.println("Erro no rollback: " + ex.getMessage());
                    }
                }

                System.err.println("Erro ao atualizar categoria: " + e.getMessage());

                result = new Resposta(
                        500,
                        "{\"erro\":\"Falha ao atualizar categoria.\"}"
                );

            } finally {

                if (conn != null) {

                    try {

                        conn.close();

                    } catch (SQLException e) {

                        System.err.println("Erro ao fechar conexao: " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    public Resposta excluir(String auth, int id) {

        Resposta result;

        if (!usuarioTemPermissao(auth, "GESTAO_EVENTOS")) {

            result = new Resposta(403, "{\"erro\":\"Acesso negado.\"}");

        } else {

            Connection conn = null;

            try {

                conn = Conexao.getConexao();

                conn.setAutoCommit(false);

                CategoriaEvento categoria =
                        CategoriaEvento.buscarPorId(conn, id);

                if (categoria == null) {

                    conn.rollback();

                    result = new Resposta(
                            404,
                            "{\"erro\":\"Categoria nÃ£o encontrada\"}"
                    );

                } else {

                    if (CategoriaEvento.deletar(conn, id)) {

                        conn.commit();

                        result = new Resposta(
                                200,
                                "{\"mensagem\":\"Categoria removida com sucesso\"}"
                        );

                    } else {

                        conn.rollback();

                        result = new Resposta(
                                500,
                                "{\"erro\":\"Erro ao remover categoria\"}"
                        );
                    }
                }

            } catch (Exception e) {

                if (conn != null) {

                    try {

                        conn.rollback();

                    } catch (SQLException ex) {

                        System.err.println("Erro no rollback: " + ex.getMessage());
                    }
                }

                System.err.println("Erro ao excluir categoria: " + e.getMessage());

                result = new Resposta(
                        500,
                        "{\"erro\":\"Falha ao excluir categoria.\"}"
                );

            } finally {

                if (conn != null) {

                    try {

                        conn.close();

                    } catch (SQLException e) {

                        System.err.println("Erro ao fechar conexao: " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    private String escaparJson(String s) {

        String res;

        if (s == null) {

            res = "";

        } else {

            res = s
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        return res;
    }
}