package org.example.sigem.service;

import org.example.sigem.model.Usuario;
import org.example.sigem.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepository usuarioRepository;

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Usuario findByCpf(String cpf) {
        return usuarioRepository.findByCpf(cpf);
    }

    public boolean inserir(Usuario usuario) {
        try {
            usuarioRepository.save(usuario);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
