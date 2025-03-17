package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF;

import by.bsuir.aleksandrov.recommendeddiploma.model.*;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import io.lettuce.core.dynamic.annotation.CommandNaming;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class Tf_IDF_RecommendationAlgorithm implements RecommendationAlgorithm {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ProductSimilarityCalculator productSimilarityCalculator;

    public Tf_IDF_RecommendationAlgorithm(SchemaRepository schemaRepository, ItemRepository itemRepository, UserRepository userRepository) throws IOException {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        Schema schema = schemaRepository.findByEntityType("Item").orElseThrow(RuntimeException::new);//TODO exception
        productSimilarityCalculator = new ProductSimilarityCalculator(schema.getFields());
    }

    @Override
    public boolean supports(String algorithmType) {
        return algorithmType.equals(RecommendationAlgorithmType.TF_IDF.toString());
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset) {
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found!");//TODO exception
        }
        List<String> topNKeys;
        if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {

            List<String> knownItems = user.getPreferences().stream().map(Preference::getItemId).toList();

            List<Item> itemsList = itemRepository.findAll().stream().filter(product -> !knownItems.contains(product.getItemId())).toList();

            List<Item> favouriteProducts = user.getPreferences().stream().filter(Preference::isLiked)
                    .map(product -> itemRepository.findByItemId(product.getItemId()))
                    .filter(Optional::isPresent).map(Optional::get).toList();

            Map<String, Double> similarityMap = new HashMap<>();

            for (Item product : itemsList) {
                similarityMap.put(product.getItemId(), 0.0);
            }
            try {
                productSimilarityCalculator.initializeIndex(itemsList);
                for (Item favouriteProduct : favouriteProducts) {
                    for (String productId : similarityMap.keySet()) {
                        similarityMap.put(productId, similarityMap.get(productId) +
                                productSimilarityCalculator.calculateSimilarity(favouriteProduct,
                                        itemRepository.findByItemId(productId).orElse(null)));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Some troubles!"); //TODO exception
            }

            topNKeys = similarityMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();
        } else {
            topNKeys = itemRepository.findAll().stream().map(Item::getItemId).toList();
        }

        int endIndex = Math.min(offset + limit, topNKeys.size());

        if (offset > endIndex) {
            throw new RuntimeException("Products not found!"); //TODO exception
        }

        return topNKeys.subList(offset, endIndex);
    }

    @Override
    public double evaluateModel() {
        return 0;
    }
}
