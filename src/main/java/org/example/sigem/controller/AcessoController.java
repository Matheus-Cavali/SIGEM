package org.example.sigem.controller;

import org.example.sigem.model.Colaborador;
import org.example.sigem.model.Usuario;
import org.example.sigem.model.Voluntario;
import org.example.sigem.security.JWTTokenProvider;
import org.example.sigem.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("acesso")
@CrossOrigin("*")
public class AcessoController {
    @Autowired
    UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestParam String email, @RequestParam String senha) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario usuario = usuarioService.findByEmail(email);
        if (usuario != null) {
            if (encoder.matches(senha, usuario.getSenha())) {
                String token = JWTTokenProvider.generateToken(usuario.getCpf(), usuario.getTipoUsuario());
                return  new ResponseEntity<>(token, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("E-mail ou senha invalidos", HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<Object> cadastrar(@RequestParam String nome, @RequestParam String email, @RequestParam String senha, @RequestParam String cpf, @RequestParam String tipoUsuario, @RequestParam String nivelAcesso, @RequestParam String dataInicio) {
        Usuario usuario = usuarioService.findByCpf(cpf);

        if(usuario == null) {
            DateTimeFormatter formatadorDireto = DateTimeFormatter.ofPattern("ddMMyyyy");
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            int nivel = 0;
            if(nivelAcesso == "total") {
                nivel = 1;
            }
            else nivel = 2;
            String senhaCriptografada = encoder.encode(senha);
            if(tipoUsuario.equalsIgnoreCase("Colaborador")) {
                Colaborador colab = new Colaborador();
                colab.setNome(nome);
                colab.setEmail(email);
                colab.setSenha(senhaCriptografada);
                colab.setCpf(cpf);
                colab.setTipoUsuario(tipoUsuario.toUpperCase());
                colab.setNivelAcesso(nivel);
                colab.setStatusAtivo(true);
                colab.setDataAdmissao(LocalDate.parse(dataInicio, formatadorDireto));
                if(usuarioService.inserir(colab))
                    return ResponseEntity.status(HttpStatus.CREATED).body("Colaborador cadastrado!");
            }
            else if(tipoUsuario.equalsIgnoreCase("VOLUNTARIO")) {
                Voluntario volun = new Voluntario();
                volun.setNome(nome);
                volun.setEmail(email);
                volun.setSenha(senhaCriptografada);
                volun.setCpf(cpf);
                volun.setTipoUsuario(tipoUsuario.toUpperCase());
                volun.setNivelAcesso(nivel);
                volun.setStatusAtivo(true);
                volun.setDataInicio(LocalDate.parse(dataInicio, formatadorDireto));

                usuarioService.inserir(volun);
                return ResponseEntity.status(HttpStatus.CREATED).body("Voluntário cadastrado!");
            }
        }
        else {
            return ResponseEntity.badRequest().body("Tipo de usuário inválido.");
        }
        return null;
    }
}
