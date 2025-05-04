package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseRecommendationAlgorithmTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    private TestRecommendationAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        algorithm = new TestRecommendationAlgorithm();
        algorithm.userRepository = userRepository;
        algorithm.itemRepository = itemRepository;
    }

    @Test
    void testEvaluateModel_withValidUsers() throws Exception {
        List<User> users = List.of(
                createUser("user1", List.of("item1", "item2")),
                createUser("user2", List.of("item2", "item3"))
        );
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(itemRepository.findAll()).thenReturn(List.of(new Item(), new Item(), new Item(), new Item()));

        Map<String, Double> result = algorithm.evaluateModel(3, new RecommendationSettings());

        assertNotNull(result);
        assertTrue(result.containsKey("precision"));
        assertTrue(result.get("precision") >= 0);
    }

    private User createUser(String userId, List<String> itemIds) {
        User user = new User();
        user.setUserId(userId);

        List<Preference> preferences = new ArrayList<>();
        for (String itemId : itemIds) {
            Preference pref = new Preference();
            pref.setItemId(itemId);
            pref.setRating(5);
            preferences.add(pref);
        }

        user.setPreferences(preferences);
        return user;
    }

    // Тестовая реализация алгоритма с захардкоженными рекомендациями
    static class TestRecommendationAlgorithm extends BaseRecommendationAlgorithm {
        @Override
        public boolean supports(String algorithmType) {
            return true;
        }

        @Override
        public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                                    RecommendationSettings settings, boolean useCache) {
            // Для теста возвращаем одни и те же рекомендации
            return List.of("item1", "item2", "item4");
        }

        @Override
        public String retrainModel() throws Exception {
            return "";
        }
    }
}

