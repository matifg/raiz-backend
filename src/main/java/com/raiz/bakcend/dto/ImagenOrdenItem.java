package com.raiz.bakcend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ImagenOrdenItem {
    private UUID id;
    private Integer orden;
}
