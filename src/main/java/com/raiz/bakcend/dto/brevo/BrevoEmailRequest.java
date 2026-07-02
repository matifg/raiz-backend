package com.raiz.bakcend.dto.brevo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class BrevoEmailRequest {
    private Sender sender;
    private List<Recipient> to;
    private String subject;
    private String htmlContent;

    @Data
    @AllArgsConstructor
    public static class Sender {
        private String name;
        private String email;
    }

    @Data
    @AllArgsConstructor
    public static class Recipient {
        private String email;
    }
}
