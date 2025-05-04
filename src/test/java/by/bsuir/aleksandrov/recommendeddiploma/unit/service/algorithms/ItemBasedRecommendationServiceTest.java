package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.item_based.ItemBasedRecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemBasedRecommendationServiceTest {

    private ItemBasedRecommendationService service;
    private RedisService redisService;
    private DataLoader dataLoader;

    @BeforeEach
    void setUp() throws Exception {
        service = new ItemBasedRecommendationService();
        redisService = mock(RedisService.class);
        dataLoader = mock(DataLoader.class);

        service.redisService = redisService;
        service.dataLoader = dataLoader;
        service.retrainModel(); // init dataModel = null
    }

    @Test
    void testSupports_itemBased() {
        assertTrue(service.supports("item_based"));
        assertTrue(service.supports("ITEM_BASED"));
        assertFalse(service.supports("svd"));
    }

    @Test
    void testGenerateRecommendations_fromCache() throws Exception {
        String userId = "1";
        int limit = 5;
        List<String> cached = List.of("101", "102");
        String cacheKey = "key1";

        when(redisService.generateKey(userId, limit, 0, true, "item-based")).thenReturn(cacheKey);
        when(redisService.getCachedRecommendations(cacheKey)).thenReturn(cached);

        List<String> result = service.generateRecommendations(userId, limit, 0, true, new RecommendationSettings(), true);
        assertEquals(cached, result);
        verify(redisService, never()).cacheRecommendations(any(), any(), anyInt());
    }

    @Test
    void testGenerateRecommendations_calculatesAndCaches() throws Exception {
        String userId = "2";
        int limit = 2;
        String cacheKey = "someKey";
        RecommendedItem mockItem = mock(RecommendedItem.class);
        when(mockItem.getItemID()).thenReturn(200L);

        when(redisService.generateKey(userId, limit, 0, true, "item-based")).thenReturn(cacheKey);
        when(redisService.getCachedRecommendations(cacheKey)).thenReturn(null);
        when(dataLoader.getDataModel()).thenReturn(MockDataModels.simpleModel());

        List<String> result = service.generateRecommendations(userId, limit, 0, true, new RecommendationSettings(), true);

        assertNotNull(result);
    }

    @Test
    void testGenerateRecommendations_noFiltering() throws Exception {
        String userId = "3";
        int limit = 2;

        when(redisService.generateKey(userId, limit, 0, false, "item-based")).thenReturn("keyNoFilter");
        when(dataLoader.getDataModel()).thenReturn(MockDataModels.simpleModel());
        when(redisService.getCachedRecommendations(any())).thenReturn(null);

        List<String> result = service.generateRecommendations(userId, limit, 0, false, new RecommendationSettings(), false);
        assertNotNull(result);
    }

    @Test
    void testRetrainModel_returnsExpectedMessage() {
        assertEquals("Not SVD algorithm", service.retrainModel());
    }
}

