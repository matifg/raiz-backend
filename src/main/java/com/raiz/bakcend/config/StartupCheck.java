package com.raiz.bakcend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupCheck implements CommandLineRunner {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Override
    public void run(String... args) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.out.println("Brevo API Key NO cargada");
            return;
        }
        System.out.println("Brevo API Key cargada correctamente: " + enmascarar(brevoApiKey));
    }

    private String enmascarar(String key) {
        if (key.length() <= 12) {
            return "******";
        }
        return key.substring(0, 12) + "******";
    }
}
