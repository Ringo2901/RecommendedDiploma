package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationAlgorithmType;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.*;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RecommendationSettingsRepository settingsRepository;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveModel_shouldSaveToRedis() {
        redisService.saveModel("key1", "model");
        verify(valueOperations).set("key1", "model");
    }

    @Test
    void getModel_shouldReturnModelFromRedis() {
        when(valueOperations.get("key1")).thenReturn("model");
        Object result = redisService.getModel("key1");
        assertEquals("model", result);
    }

    @Test
    void deleteModel_shouldCallRedisDelete() {
        redisService.deleteModel("key1");
        verify(redisTemplate).delete("key1");
    }

    @Test
    void exists_shouldReturnTrueIfExists() {
        when(redisTemplate.hasKey("key1")).thenReturn(true);
        assertTrue(redisService.exists("key1"));
    }

    @Test
    void exists_shouldReturnFalseIfNull() {
        when(redisTemplate.hasKey("key1")).thenReturn(null);
        assertFalse(redisService.exists("key1"));
    }

    @Test
    void cacheRecommendations_shouldSetWithTTL() {
        List<String> recs = List.of("item1", "item2");
        redisService.cacheRecommendations("key1", recs, 60);
        verify(valueOperations).set("key1", recs, Duration.ofSeconds(60));
    }

    @Test
    void getCachedRecommendations_shouldReturnList() {
        List<String> mockList = List.of("a", "b");
        when(valueOperations.get("key1")).thenReturn(mockList);
        List<String> result = redisService.getCachedRecommendations("key1");
        assertEquals(mockList, result);
    }

    @Test
    void getCachedRecommendations_shouldReturnNullIfNotList() {
        when(valueOperations.get("key1")).thenReturn("not a list");
        assertNull(redisService.getCachedRecommendations("key1"));
    }

    @Test
    void generateKey_shouldReturnCorrectFormat() {
        String key = redisService.generateKey("u1", 5, 10, true, "svd");
        assertEquals("recommendation:u1:5:10:true:svd", key);
    }

    @Test
    void evictRecommendationsByAlgorithm_shouldDeleteMatchingKeys() {
        Set<String> keys = new HashSet<>(Set.of("recommendation:user1:5:0:true:svd"));
        when(redisTemplate.keys("recommendation:*:*:*:*:svd")).thenReturn(keys);
        redisService.evictRecommendationsByAlgorithm("svd");
        verify(redisTemplate).delete(keys);
    }

    @Test
    void evictAllRecommendations_shouldDeleteAllRecommendationKeys() {
        Set<String> keys = new HashSet<>(Set.of("recommendation:a", "recommendation:b"));
        when(redisTemplate.keys("recommendation:*")).thenReturn(keys);
        redisService.evictAllRecommendations();
        verify(redisTemplate).delete(keys);
    }

    @Test
    void getRecommendationCacheSizesByAlgorithm_shouldGroupByAlgorithm() {
        Set<String> keys = Set.of(
                "recommendation:u1:5:0:true:svd",
                "recommendation:u2:5:0:true:svd",
                "recommendation:u3:5:0:true:user_based"
        );
        when(redisTemplate.keys("recommendation:*")).thenReturn(keys);

        Map<String, Integer> result = redisService.getRecommendationCacheSizesByAlgorithm();

        assertEquals(2, result.get("svd"));
        assertEquals(1, result.get("user_based"));
    }

    @Test
    void clearAll_shouldDeleteAllKeys() {
        Set<String> keys = Set.of("recommendation:x", "someKey");
        when(redisTemplate.keys("*")).thenReturn(keys);
        redisService.clearAll();
        verify(redisTemplate).delete(keys);
    }
}

