package org.example.facade;

import org.example.dao.UsuarioDao;
import org.example.model.Usuario;

public class UsuarioFacade {

    public String validarCadastro(String email, String cpf) {
        UsuarioDao dao = new UsuarioDao();

        if (dao.buscarPorEmail(email) != null) {
            return "Email já cadastrado no sistema";
        }

        if (dao.buscarPorCPF(cpf) != null) {
            return "CPF já cadastrado no sistema";
        }

        return null;
    }

    public String validarDesativacao(int id) {
        UsuarioDao dao = new UsuarioDao();
        Usuario usuario = dao.buscarPorId(id);

        if (usuario == null) {
            return "Usuário não encontrado";
        }

        if ("colaborador".equalsIgnoreCase(usuario.getTipoUsuario()) && usuario.getNivelAcesso() == 1) {
            int totalAtivos = dao.contarColaboradorAcessoTotalAtivo();
            if (totalAtivos <= 1) {
                return "Não é possível desativar o único colaborador com acesso total";
            }
        }

        return null;
    }
}