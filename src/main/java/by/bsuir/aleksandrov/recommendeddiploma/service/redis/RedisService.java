package by.bsuir.aleksandrov.recommendeddiploma.service.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveModel(String key, Object model) {
        redisTemplate.opsForValue().set(key, model);
    }

    public Object getModel(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteModel(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public List<String> getCachedRecommendations(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }

    public void cacheRecommendations(String key, List<String> recommendations, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, recommendations, Duration.ofSeconds(ttlSeconds));
    }

    public String generateKey(String userId, int limit, int offset, boolean filtering, String algorithmName) {
        return String.format("recommendation:%s:%d:%d:%b:%s", userId, limit, offset, filtering, algorithmName);
    }


    // 👇 Метод 1: Удаление всех рекомендаций по алгоритму
    public void evictRecommendationsByAlgorithm(String algorithmName) {
        Set<String> keys = redisTemplate.keys("recommendation:*:*:*:*:" + algorithmName);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // 👇 Метод 2: Удаление всех рекомендаций (любых алгоритмов)
    public void evictAllRecommendations() {
        Set<String> keys = redisTemplate.keys("recommendation:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
