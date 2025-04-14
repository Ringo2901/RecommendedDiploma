package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserBasedRecommendationService extends BaseRecommendationAlgorithm {
    @Autowired
    private DataLoader dataLoader;
    private DataModel dataModel;
    @Autowired
    private RedisService redisService;

    @Override
    public boolean supports(String algorithmType) {
        return "user_based".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                                RecommendationSettings settings) throws Exception {
        String cacheKey = redisService.generateKey(userId, limit, offset, filtering, "user-based");

        List<String> cached = redisService.getCachedRecommendations(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RecommendedItem> recommendedItems = calculateRecommendations(userId, limit, filtering,
                Integer.parseInt(settings.getParameters().get("numNeighbors").toString()));
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }

        redisService.cacheRecommendations(cacheKey, recommendations, 1800);
        return recommendations;
    }

    private List<RecommendedItem> calculateRecommendations(String userId, int limit, boolean filtering,
                                                           Integer numNeighborhood) throws Exception {
        if (dataModel == null) {
            dataModel = dataLoader.getDataModel();
        }

        try {
            UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighborhood, similarity, dataModel);
            Recommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, similarity);

            if (filtering) {
                return recommender.recommend(Long.parseLong(userId), limit);
            } else {
                return recommender.recommend(Long.parseLong(userId), limit, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


    @Override
    public String retrainModel() {
        return "Not SVD algorithm";
    }
}
