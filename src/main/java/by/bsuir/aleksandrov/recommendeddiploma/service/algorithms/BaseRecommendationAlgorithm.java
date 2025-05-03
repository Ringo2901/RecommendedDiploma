package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms;
import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import com.google.common.util.concurrent.AtomicDouble;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public abstract class BaseRecommendationAlgorithm implements RecommendationAlgorithm {

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ItemRepository itemRepository;

    @Override
    public Map<String, Double> evaluateModel(int limit, RecommendationSettings settings) {
        Pageable pageable = PageRequest.of(0, 20);
        List<User> users = userRepository.findAll(pageable).getContent();

        List<Set<String>> allRecommendations = Collections.synchronizedList(new ArrayList<>());
        Set<String> allRecommendedItems = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger totalTruePositives = new AtomicInteger(0);
        AtomicInteger totalFalsePositives = new AtomicInteger(0);
        AtomicInteger totalFalseNegatives = new AtomicInteger(0);
        AtomicInteger totalRelevantItems = new AtomicInteger(0);
        AtomicInteger hitUsers = new AtomicInteger(0);

        // Для nDCG
        AtomicDouble dcgSum = new AtomicDouble(0);
        AtomicDouble idcgSum = new AtomicDouble(0);

        users.parallelStream().forEach(user -> {
            if (user.getPreferences() != null && !user.getPreferences().isEmpty()) {
                List<String> knownItems = user.getPreferences() != null ?
                        user.getPreferences().stream().map(Preference::getItemId).toList() : Collections.emptyList();

                int totalKnownItems = knownItems.size();
                totalRelevantItems.addAndGet(totalKnownItems);

                List<String> recommendations;
                try {
                    recommendations = generateRecommendations(user.getUserId(), limit, 0, false, settings, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                Set<String> recommendedSet = new HashSet<>(recommendations);
                allRecommendations.add(new HashSet<>(recommendations));
                allRecommendedItems.addAll(recommendedSet);

                // Precision, Recall, F1-score
                int tp = (int) recommendedSet.stream().filter(knownItems::contains).count();
                int fp = recommendedSet.size() - tp;
                int fn = Math.min(knownItems.size() - tp, limit - tp);

                totalTruePositives.addAndGet(tp);
                totalFalsePositives.addAndGet(fp);
                totalFalseNegatives.addAndGet(fn);

                // Для Hit Rate
                if (tp > 0) {
                    hitUsers.incrementAndGet();
                }

                // Для nDCG
                double userDCG = 0;
                double userIDCG = 0;
                for (int i = 0; i < recommendations.size(); i++) {
                    if (knownItems.contains(recommendations.get(i))) {
                        userDCG += 1.0 / (Math.log(i + 2) / Math.log(2)); // 1-based index
                    }
                    if (i < totalKnownItems) {
                        userIDCG += 1.0 / (Math.log(i + 2) / Math.log(2)); // Ideal DCG
                    }
                }
                dcgSum.addAndGet(userDCG);
                idcgSum.addAndGet(userIDCG);
            }
        });

        // Precision, Recall, F1-Score
        double precision = totalTruePositives.get() == 0 ? 0 : (double) totalTruePositives.get()
                / (totalTruePositives.get() + totalFalsePositives.get());
        double recall = totalTruePositives.get() + totalFalseNegatives.get() == 0 ? 0 : (double) totalTruePositives.get()
                / (totalTruePositives.get() + totalFalseNegatives.get());
        double f1Score = (precision + recall) == 0 ? 0 : 2 * (precision * recall) / (precision + recall);

        // nDCG
        double nDCG = idcgSum.get() == 0 ? 0 : dcgSum.get() / idcgSum.get();

        // Hit Rate
        double hitRate = (double) hitUsers.get() / users.size();

        // Coverage
        double totalItems = itemRepository.findAll().size();
        double coverage = totalItems == 0 ? 0 : (double) allRecommendedItems.size() / totalItems;

        // Personalization
        double personalizationSum = allRecommendations.parallelStream()
                .flatMapToDouble(set1 -> allRecommendations.stream()
                        .filter(set2 -> set1 != set2)
                        .mapToDouble(set2 -> {
                            double jaccardSimilarity = (double) intersection(set1, set2).size() / union(set1, set2).size();
                            return 1 - jaccardSimilarity;
                        })
                ).sum();
        int numPairs = (allRecommendations.size() * (allRecommendations.size() - 1)) / 2;
        double personalization = numPairs == 0 ? 0 : personalizationSum / numPairs;

        Map<String, Double> result = new HashMap<>();
        result.put("personalization", personalization / 2);
        result.put("precision", precision);
        result.put("recall", recall);
        result.put("f1Score", f1Score);
        result.put("nDCG", nDCG);
        result.put("hitRate", hitRate);
        result.put("coverage", coverage);

        return result;
    }

    private Set<String> intersection(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return intersection;
    }

    private Set<String> union(Set<String> set1, Set<String> set2) {
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return union;
    }

    public abstract List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                                         RecommendationSettings settings, boolean useCache) throws Exception;
}
