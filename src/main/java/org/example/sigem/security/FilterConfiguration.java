//package org.example.sigem.security;
//
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Configuration
//public class FilterConfiguration {
//
//    @Bean
//    public FilterRegistrationBean<CorsFilter> corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
//
//        config.addAllowedMethod(HttpMethod.PUT);
//        config.addAllowedMethod(HttpMethod.DELETE);
//        config.addAllowedMethod(HttpMethod.POST);
//        config.addAllowedMethod(HttpMethod.GET);
//
//        source.registerCorsConfiguration("/**", config);
//        FilterRegistrationBean<CorsFilter> register = new FilterRegistrationBean<>(new CorsFilter(source));
//
//        register.setOrder(0);
//        return register;
//    }
//
//    @Bean
//    public FilterRegistrationBean<AccessFilter> registrationBean() {
//        FilterRegistrationBean<AccessFilter> register = new FilterRegistrationBean<>();
//
//        register.setFilter(new AccessFilter());
//
//        register.addUrlPatterns("/apis/colaborador/*");
//        register.addUrlPatterns("/apis/voluntario/*");
//
//        register.setOrder(1);
//        return register;
//    }
//}