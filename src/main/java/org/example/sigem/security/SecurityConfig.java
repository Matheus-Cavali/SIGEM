package org.example.sigem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Desliga a proteção CSRF (obrigatório para APIs REST que usam Token)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Libera todas as rotas no Spring Security (Quem vai bloquear é o seu AccessFilter!)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 3. DESLIGA A TELA PRETA DE LOGIN HTML
                .formLogin(AbstractHttpConfigurer::disable)

                // 4. Desliga o popup de login do navegador
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}