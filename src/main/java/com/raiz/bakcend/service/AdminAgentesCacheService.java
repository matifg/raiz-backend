package com.raiz.bakcend.service;

import com.raiz.bakcend.config.CacheConfig;
import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class AdminAgentesCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAgentesCacheService.class);

    private final CacheManager cacheManager;

    public AdminAgentesCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public String buildKey(int page, int size) {
        return "page=" + page + "::size=" + size;
    }

    public AgenteAdminPageResponse get(int page, int size) {
        String key = buildKey(page, size);
        Cache.ValueWrapper cached = getCache().get(key);

        if (cached == null) {
            logger.info("[ADMIN_CACHE] MISS cache={} key={}", CacheConfig.ADMIN_AGENTES_CACHE, key);
            return null;
        }

        logger.info("[ADMIN_CACHE] HIT cache={} key={}", CacheConfig.ADMIN_AGENTES_CACHE, key);
        return (AgenteAdminPageResponse) cached.get();
    }

    public void put(int page, int size, AgenteAdminPageResponse response) {
        String key = buildKey(page, size);
        getCache().put(key, response);
        logger.info("[ADMIN_CACHE] STORED cache={} key={}", CacheConfig.ADMIN_AGENTES_CACHE, key);
    }

    public void evictAll(String reason) {
        getCache().clear();
        logger.info("[ADMIN_CACHE] EVICT cache={} reason={}", CacheConfig.ADMIN_AGENTES_CACHE, reason);
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CacheConfig.ADMIN_AGENTES_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Cache no configurada: " + CacheConfig.ADMIN_AGENTES_CACHE);
        }
        return cache;
    }
}
