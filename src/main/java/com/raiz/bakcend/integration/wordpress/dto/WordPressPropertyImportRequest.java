package com.raiz.bakcend.integration.wordpress.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WordPressPropertyImportRequest {

    private String direccion;
    private BigDecimal precio;
    private Integer tipoId;
}
