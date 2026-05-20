package com.raiz.bakcend.config;

import com.raiz.bakcend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth
        // Permitir GET públicos
        .requestMatchers(org.springframework.http.HttpMethod.GET, "/propiedades/**").permitAll()
        .requestMatchers(org.springframework.http.HttpMethod.GET, "/imagenes/**").permitAll()
        // Permitir login y register públicos
        .requestMatchers("/auth/login").permitAll()
        .requestMatchers("/auth/register").permitAll()
        // Requerir autenticación para POST
        .requestMatchers(org.springframework.http.HttpMethod.POST, "/propiedades/**").authenticated()
        .requestMatchers(org.springframework.http.HttpMethod.POST, "/imagenes/**").authenticated()
        // El resto requiere autenticación
        .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter(),
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .httpBasic(org.springframework.security.config.Customizer.withDefaults());
    return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://192.168.0.13:3000",
                "https://inmobiliaria360.com.ar",
                "https://inmportal.pages.dev"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}