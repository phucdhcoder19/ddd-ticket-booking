package com.hoangphuc.ddd.infrastructure.cache.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisInfrasServiceImpl implements RedisInfrasService {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void setObject(String key, Object value) {
        setObject(key, value, null);
    }

    @Override
    public void setObject(String key, Object value, Duration ttl) {
        if (!StringUtils.hasLength(key) || value == null) {   // chú ý dấu "!"
            return;
        }
        try {
            if (ttl == null) {
                redisTemplate.opsForValue().set(key, value);
            } else {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
        } catch (Exception e) {
            log.error("setObject error: key={}", key, e);
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        if (!StringUtils.hasLength(key)) {
            return null;
        }
        Object result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            return null;
        }
        if (targetClass.isInstance(result)) {
            return targetClass.cast(result);
        }
        try {
            // Jackson trả LinkedHashMap khi deserialize về Object → convert sang class đích
            return MAPPER.convertValue(result, targetClass);
        } catch (IllegalArgumentException e) {
            log.error("getObject convert error: key={} target={}", key, targetClass.getSimpleName(), e);
            return null;
        }
    }

    @Override
    public void setInt(String key, int value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public int getInt(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return -1;                      // cache miss — KHÔNG được để NPE
        }
        return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public Long executeScript(RedisScript<Long> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
