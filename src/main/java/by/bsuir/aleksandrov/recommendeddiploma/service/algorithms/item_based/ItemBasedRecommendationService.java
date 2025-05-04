package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.item_based;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ItemBasedRecommendationService extends BaseRecommendationAlgorithm {
    @Autowired
    public DataLoader dataLoader;
    @Autowired
    public RedisService redisService;
    private DataModel dataModel;

    @Override
    public boolean supports(String algorithmType) {
        return "item_based".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                                RecommendationSettings settings, boolean useCache) throws Exception {
        String cacheKey = redisService.generateKey(userId, limit, offset, filtering, "item-based");
        if (useCache) {
            List<String> cached = redisService.getCachedRecommendations(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        List<RecommendedItem> recommendedItems = calculateRecommendations(userId, limit, filtering);
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }
        if (useCache) {
            redisService.cacheRecommendations(cacheKey, recommendations, 1800);
        }
        return recommendations;
    }

    @Override
    public String retrainModel() {
        return "Not SVD algorithm";
    }

    private List<RecommendedItem> calculateRecommendations(String userId, int limit, boolean filtering) throws Exception {
        if (dataModel == null) {
            dataModel = dataLoader.getDataModel();
        }

        try {
            ItemSimilarity itemSimilarity = new UncenteredCosineSimilarity(dataModel);
            Recommender recommender = new GenericItemBasedRecommender(dataModel, itemSimilarity);

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


}

