package com.rrtechnosoft.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * The project pulled in spring-boot-starter-data-redis but never actually
 * used it anywhere (no RedisTemplate, no @Cacheable) — Redis was running in
 * docker-compose doing nothing. This wires it up for the handful of
 * genuinely hot, low-churn reads that are worth caching: master data
 * dropdowns (every module's forms hit these) and feature toggles (checked
 * on effectively every request that touches a gated feature).
 *
 * Deliberately NOT cached: anything student- or course-specific (progress,
 * grades, enrollments, payments) — those change often enough and are
 * sensitive enough that a stale cache is a worse trade than an extra query.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MASTER_DATA_CATEGORIES = "masterDataCategories";
    public static final String MASTER_DATA_ITEMS = "masterDataItems";
    public static final String FEATURE_TOGGLES = "featureToggles";
    public static final String FEATURE_TOGGLE_STATUS = "featureToggleStatus";

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper baseMapper) {
        ObjectMapper mapper = baseMapper.copy();
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return builder -> builder
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        MASTER_DATA_CATEGORIES, defaultConfig.entryTtl(Duration.ofHours(1)),
                        MASTER_DATA_ITEMS, defaultConfig.entryTtl(Duration.ofHours(1)),
                        FEATURE_TOGGLES, defaultConfig.entryTtl(Duration.ofMinutes(5)),
                        FEATURE_TOGGLE_STATUS, defaultConfig.entryTtl(Duration.ofMinutes(5))
                ));
    }
}
