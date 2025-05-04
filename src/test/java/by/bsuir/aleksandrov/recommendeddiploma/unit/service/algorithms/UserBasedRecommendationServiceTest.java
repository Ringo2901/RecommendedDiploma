package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based.UserBasedRecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserBasedRecommendationServiceTest {

    @InjectMocks
    private UserBasedRecommendationService recommendationService;

    @Mock
    private RedisService redisService;

    @Mock
    private DataLoader dataLoader;

    @Mock
    private DataModel dataModel;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(dataLoader.getDataModel()).thenReturn(dataModel);
    }

    @Test
    void supports_shouldReturnTrueForUserBased() {
        assertTrue(recommendationService.supports("user_based"));
        assertTrue(recommendationService.supports("USER_BASED"));
    }

    @Test
    void supports_shouldReturnFalseForOther() {
        assertFalse(recommendationService.supports("svd"));
    }

    @Test
    void generateRecommendations_shouldReturnCachedRecommendations() throws Exception {
        List<String> cached = List.of("1", "2");
        when(redisService.generateKey("user1", 5, 0, true, "user-based")).thenReturn("cache-key");
        when(redisService.getCachedRecommendations("cache-key")).thenReturn(cached);

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numNeighbors", 3));

        List<String> result = recommendationService.generateRecommendations("user1", 5, 0, true, settings, true);
        assertEquals(cached, result);
    }

    @Test
    void generateRecommendations_shouldComputeAndCacheIfNoCache() throws Exception {
        String userId = "1";
        List<RecommendedItem> mockItems = List.of(mockRecommendedItem(100), mockRecommendedItem(200));
        String key = "cache-key";

        when(redisService.generateKey(userId, 2, 0, true, "user-based")).thenReturn(key);
        when(redisService.getCachedRecommendations(key)).thenReturn(null);

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numNeighbors", 2));

        // Spy to mock internal method
        UserBasedRecommendationService spyService = Mockito.spy(recommendationService);
        doReturn(mockItems).when(spyService).calculateRecommendations(userId, 2, true, 2);

        List<String> result = spyService.generateRecommendations(userId, 2, 0, true, settings, true);

        assertEquals(List.of("100", "200"), result);
        verify(redisService).cacheRecommendations(key, List.of("100", "200"), 1800);
    }

    @Test
    void retrainModel_shouldReturnNotSvdMessage() {
        assertEquals("Not SVD algorithm", recommendationService.retrainModel());
    }

    // 🔧 Вспомогательный метод
    private RecommendedItem mockRecommendedItem(long id) {
        RecommendedItem item = mock(RecommendedItem.class);
        when(item.getItemID()).thenReturn(id);
        return item;
    }
}

