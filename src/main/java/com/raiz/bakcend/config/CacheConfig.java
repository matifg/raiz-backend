package com.raiz.bakcend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    public static final String ADMIN_AGENTES_CACHE = "adminAgentes";
    public static final String GEOREF_PROVINCIAS_CACHE = "georefProvincias";
    public static final String GEOREF_LOCALIDADES_CACHE = "georefLocalidades";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                ADMIN_AGENTES_CACHE,
                GEOREF_PROVINCIAS_CACHE,
                GEOREF_LOCALIDADES_CACHE);
    }
}
