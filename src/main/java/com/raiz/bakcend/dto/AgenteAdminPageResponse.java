package com.raiz.bakcend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AgenteAdminPageResponse {
    private List<AgenteAdminResponse> agentes;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
