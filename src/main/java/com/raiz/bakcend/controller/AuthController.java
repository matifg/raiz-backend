package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.LoginRequest;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.repository.AgenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AgenteRepository agenteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Agente> agenteOpt = agenteRepository.findByEmail(request.getEmail());
        if (agenteOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Usuario no encontrado");
        }
        Agente agente = agenteOpt.get();
        boolean passwordCorrecto = passwordEncoder.matches(request.getPassword(), agente.getPasswordHash());
        if (!passwordCorrecto) {
            return ResponseEntity.status(401).body("Password incorrecto");
        }
        return ResponseEntity.ok(Map.of("id", agente.getId()));
    }
}
