//package org.example.sigem.security;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import java.io.IOException;
//
//@Component
//public class AccessFilter implements Filter {
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest req = (HttpServletRequest) request;
//        HttpServletResponse res = (HttpServletResponse) response;
//
//        String token = req.getHeader("Authorization");
//
//        if (token != null && JWTTokenProvider.verifyToken(token)) {
//            chain.doFilter(request, response);
//        } else {
//            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            res.setCharacterEncoding("UTF-8");
//            res.getWriter().write("Acesso negado: Token inválido ou ausente.");
//        }
//    }
//}