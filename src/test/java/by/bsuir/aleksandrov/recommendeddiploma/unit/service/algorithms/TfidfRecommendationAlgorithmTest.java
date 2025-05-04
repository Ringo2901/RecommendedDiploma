package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.*;
import by.bsuir.aleksandrov.recommendeddiploma.repository.*;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF.ProductSimilarityCalculator;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF.Tf_IDF_RecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TfidfRecommendationAlgorithmTest {

    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private SchemaRepository schemaRepository;
    @Mock private RedisService redisService;
    @Mock private ProductSimilarityCalculator calculator;

    @InjectMocks
    private Tf_IDF_RecommendationAlgorithm algorithm;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Мок схема и инициализация калькулятора
        Schema schema = new Schema();
        schema.setFields(List.of(new SchemaField("name", "string")));
        when(schemaRepository.findByEntityType("Item")).thenReturn(Optional.of(schema));

        algorithm = new Tf_IDF_RecommendationAlgorithm(schemaRepository, itemRepository, userRepository);
        algorithm.redisService = redisService;
    }

    @Test
    void supports_shouldReturnTrueForTFIDF() {
        assertTrue(algorithm.supports("TF_IDF"));
    }

    @Test
    void retrainModel_shouldReturnNotSvd() {
        assertEquals("Not SVD algorithm", algorithm.retrainModel());
    }

    @Test
    void generateRecommendations_shouldReturnCached() throws IOException {
        List<String> cached = List.of("1", "2");
        when(redisService.generateKey("u1", 2, 0, true, "tf-idf")).thenReturn("key");
        when(redisService.getCachedRecommendations("key")).thenReturn(cached);

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numItems", 5));

        List<String> result = algorithm.generateRecommendations("u1", 2, 0, true, settings, true);
        assertEquals(cached, result);
    }

    @Test
    void generateRecommendations_shouldReturnItemsWithoutPreferences() throws IOException {
        User user = new User();
        user.setUserId("u1");
        user.setPreferences(null);

        List<Item> allItems = List.of(new Item("i1", null), new Item("i2", null), new Item("i3", null));

        when(redisService.generateKey(any(), anyInt(), anyInt(), anyBoolean(), anyString())).thenReturn("key");
        when(redisService.getCachedRecommendations("key")).thenReturn(null);
        when(userRepository.findByUserId("u1")).thenReturn(Optional.of(user));
        when(itemRepository.findAll()).thenReturn(allItems);

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numItems", 10));

        List<String> result = algorithm.generateRecommendations("u1", 2, 0, true, settings, true);

        assertEquals(List.of("i1", "i2"), result);
    }

    @Test
    void generateRecommendations_shouldThrowIfUserNotFound() {
        when(userRepository.findByUserId("uX")).thenReturn(Optional.empty());

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numItems", 10));

        assertThrows(RuntimeException.class, () -> {
            algorithm.generateRecommendations("uX", 2, 0, true, settings, false);
        });
    }

    @Test
    void generateRecommendations_shouldThrowIfOffsetTooBig() throws IOException {
        User user = new User();
        user.setUserId("u1");
        user.setPreferences(null);

        when(userRepository.findByUserId("u1")).thenReturn(Optional.of(user));
        when(itemRepository.findAll()).thenReturn(List.of(new Item("i1", null)));

        RecommendationSettings settings = new RecommendationSettings();
        settings.setParameters(Map.of("numItems", 10));

        assertThrows(RuntimeException.class, () -> {
            algorithm.generateRecommendations("u1", 2, 10, true, settings, false);
        });
    }
}
