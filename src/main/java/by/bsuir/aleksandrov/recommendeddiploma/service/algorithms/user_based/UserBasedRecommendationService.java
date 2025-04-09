package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based;

import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.common.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserBasedRecommendationService extends BaseRecommendationAlgorithm {
    @Autowired
    private DataLoader dataLoader;
    private DataModel dataModel;

    @Override
    public boolean supports(String algorithmType) {
        return "user_based".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering) throws Exception {
        List<RecommendedItem> recommendedItems = calculateRecommendations(userId, limit);
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }
        return recommendations;
    }

    private List<RecommendedItem> calculateRecommendations(String userId, int limit) throws Exception {
        if (dataModel == null) {
            dataModel = dataLoader.getDataModel();
        }

        try {
            UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(75, similarity, dataModel);
            Recommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, similarity);

            return recommender.recommend(Long.parseLong(userId), limit);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
