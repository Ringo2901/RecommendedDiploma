package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.FactorizationData;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd.CustomSVDRecommender;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd.SVDRecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SVDRecommendationServiceTest {

    @InjectMocks
    private SVDRecommendationService svdService;

    @Mock private RedisService redisService;
    @Mock private DataLoader dataLoader;
    @Mock private RecommendationSettingsRepository settingsRepository;
    @Mock private DataModel dataModel;
    @Mock private Factorization factorization;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void supports_shouldReturnTrueForSVD() {
        assertTrue(svdService.supports("svd"));
    }

    @Test
    void generateRecommendations_shouldReturnRecommendationList() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numFeatures", 2));

        RecommendedItem item1 = mock(RecommendedItem.class);
        when(item1.getItemID()).thenReturn(100L);
        RecommendedItem item2 = mock(RecommendedItem.class);
        when(item2.getItemID()).thenReturn(101L);

        CustomSVDRecommender recommender = mock(CustomSVDRecommender.class);
        when(recommender.recommend(1L, 2, null, true)).thenReturn(List.of(item1, item2));

        when(redisService.exists(any())).thenReturn(false);
        when(dataLoader.getDataModel()).thenReturn(dataModel);
        when(redisService.getModel(any())).thenReturn(null);

        Factorizer factorizer = mock(Factorizer.class);
        when(factorizer.factorize()).thenReturn(factorization);

        // вручную вызываем train и создаём реализацию
        SVDRecommendationService spyService = Mockito.spy(svdService);
        doReturn(factorization).when(spyService).trainAndCacheFactorization(any(), any(), anyBoolean());
        doReturn(recommender).when(spyService).loadOrTrainRecommender(any(), anyBoolean());

        List<String> result = spyService.generateRecommendations("1", 2, 0, true, settings, false);
        assertEquals(List.of("100", "101"), result);
    }

    @Test
    void retrainModel_shouldClearCacheAndRetrain() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numFeatures", 3));

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));
        when(dataLoader.getDataModel()).thenReturn(dataModel);

        SVDRecommendationService spyService = Mockito.spy(svdService);
        doReturn(factorization).when(spyService).trainAndCacheFactorization(any(), any(), eq(true));

        String result = spyService.retrainModel();
        assertEquals("Retrain successfully", result);
        verify(redisService).deleteModel("svd-model");
        verify(redisService).evictRecommendationsByAlgorithm("svd");
    }

    @Test
    void saveAndLoadFactorizationToRedis_shouldWorkCorrectly() {
        FastByIDMap<Integer> userMap = new FastByIDMap<>();
        userMap.put(1L, 0);
        FastByIDMap<Integer> itemMap = new FastByIDMap<>();
        itemMap.put(10L, 0);
        double[][] userFeatures = new double[][]{{0.1, 0.2}};
        double[][] itemFeatures = new double[][]{{0.3, 0.4}};

        when(factorization.getUserIDMappings()).thenReturn(userMap.entrySet());
        when(factorization.getItemIDMappings()).thenReturn(itemMap.entrySet());
        when(factorization.allUserFeatures()).thenReturn(userFeatures);
        when(factorization.allItemFeatures()).thenReturn(itemFeatures);

        svdService.saveFactorizationToRedis(factorization);
        verify(redisService).saveModel(eq("svd-model"), any(FactorizationData.class));

        FactorizationData data = new FactorizationData(Map.of(1L, 0), Map.of(10L, 0), userFeatures, itemFeatures);
        when(redisService.getModel("svd-model")).thenReturn(data);

        Factorization restored = svdService.loadFactorizationFromRedis();
        assertNotNull(restored);
    }
}

