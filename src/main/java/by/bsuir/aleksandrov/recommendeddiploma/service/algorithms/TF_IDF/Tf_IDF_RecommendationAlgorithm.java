package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF;

import by.bsuir.aleksandrov.recommendeddiploma.model.*;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import com.google.common.util.concurrent.AtomicDouble;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Tf_IDF_RecommendationAlgorithm extends BaseRecommendationAlgorithm {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private ProductSimilarityCalculator productSimilarityCalculator;
    private final SchemaRepository schemaRepository;
    @Autowired
    private RedisService redisService;

    public Tf_IDF_RecommendationAlgorithm(SchemaRepository schemaRepository, ItemRepository itemRepository, UserRepository userRepository) throws IOException {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.schemaRepository = schemaRepository;
        Schema schema = schemaRepository.findByEntityType("Item").orElse(null);
        if (schema != null) {
            productSimilarityCalculator = new ProductSimilarityCalculator(schema.getFields());
        }
    }

    @Override
    public boolean supports(String algorithmType) {
        return algorithmType.equals(RecommendationAlgorithmType.TF_IDF.toString());
    }

    @Override
    public String retrainModel() {
        return "Not SVD algorithm";
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering) throws IOException {
        String cacheKey = redisService.generateKey(userId, limit, offset, filtering, "tf-idf");

        List<String> cached = redisService.getCachedRecommendations(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (productSimilarityCalculator == null) {
            Schema schema = schemaRepository.findByEntityType("Item").orElseThrow(RuntimeException::new);
            productSimilarityCalculator = new ProductSimilarityCalculator(schema.getFields());
        }

        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found!");//TODO exception
        }

        List<String> topNKeys;
        if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
            List<String> knownItems = user.getPreferences().stream().map(Preference::getItemId).limit(200).toList();

            // Параллельная фильтрация товаров
            List<Item> itemsList;
            if (filtering) {
                itemsList = itemRepository.findAll().parallelStream()
                        .filter(product -> !knownItems.contains(product.getItemId()))
                        .limit(200)
                        .toList();
            } else {
                Pageable pageable = PageRequest.of(0, 200);
                itemsList = itemRepository.findAll(pageable).getContent();
            }

            // Параллельная обработка любимых товаров
            List<Item> favouriteProducts = user.getPreferences().parallelStream()
                    .filter(Preference::isLiked)
                    .map(product -> itemRepository.findByItemId(product.getItemId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .limit(200)
                    .toList();

            Map<String, Double> similarityMap = new HashMap<>();

            // Инициализация карты сходства товаров
            itemsList.parallelStream().forEach(product -> similarityMap.put(product.getItemId(), 0.0));

            try {
                // Параллельная инициализация индекса
                productSimilarityCalculator.initializeIndex(itemsList);

                // Параллельное вычисление сходства
                favouriteProducts.parallelStream().forEach(favouriteProduct -> {
                    itemsList.parallelStream().forEach(product -> {
                        String productId = product.getItemId();
                        similarityMap.computeIfPresent(productId, (key, value) ->
                        {
                            try {
                                return value + productSimilarityCalculator.calculateSimilarity(favouriteProduct, itemRepository.findByItemId(key).orElse(null));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    });
                });
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Some troubles!"); //TODO exception
            }

            // Сортировка и выборка топ товаров
            topNKeys = similarityMap.entrySet().parallelStream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();
        } else {
            // Если пользователь не имеет предпочтений, просто возвращаем топ товаров
            topNKeys = itemRepository.findAll().parallelStream().map(Item::getItemId).limit(offset + limit).toList();
        }

        int endIndex = Math.min(offset + limit, topNKeys.size());

        if (offset > endIndex) {
            throw new RuntimeException("Products not found!"); //TODO exception
        }
        System.out.println("Recommendation for user " + userId);

        List<String> recommendations = topNKeys.subList(offset, endIndex);
        redisService.cacheRecommendations(cacheKey, recommendations, 1800);
        return recommendations;
    }

}
