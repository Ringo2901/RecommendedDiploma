package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd;

import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.RecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SVDRecommendationService extends BaseRecommendationAlgorithm {
    @Autowired
    private RedisService redisService;
    private static final String SVD_MODEL_KEY = "svd-model";
    @Autowired
    private DataLoader dataLoader;
    private DataModel dataModel;

    @Override
    public boolean supports(String algorithmType) {
        return "svd".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering) throws Exception {
        String cacheKey = redisService.generateKey(userId, limit, offset, filtering, "svd");

        List<String> cached = redisService.getCachedRecommendations(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RecommendedItem> recommendedItems = recommend(userId, limit);
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }
        redisService.cacheRecommendations(cacheKey, recommendations, 1800);
        return recommendations;
    }

    public List<RecommendedItem> recommend(String userId, int limit) throws Exception {
        Recommender recommender;

        if (redisService.exists(SVD_MODEL_KEY)) {
            recommender = (Recommender) redisService.getModel(SVD_MODEL_KEY);
        } else {
            recommender = trainAndCacheModel();
        }

        return recommender.recommend(Long.parseLong(userId), limit);
    }

    public Recommender trainAndCacheModel() throws Exception {
        dataModel = dataLoader.getDataModel();

        Factorizer factorizer = new ALSWRFactorizer(dataModel, 10, 0.05, 10);
        SVDRecommender svdRecommender = new SVDRecommender(dataModel, factorizer);

        redisService.deleteModel(SVD_MODEL_KEY);
        redisService.evictRecommendationsByAlgorithm("svd");

        redisService.saveModel(SVD_MODEL_KEY, svdRecommender);

        return svdRecommender;
    }

    @Override
    public String retrainModel() throws Exception {
        trainAndCacheModel();
        return "Retrain Successfully";
    }
}
