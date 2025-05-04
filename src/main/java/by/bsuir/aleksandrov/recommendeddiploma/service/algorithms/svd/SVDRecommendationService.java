package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd;

import by.bsuir.aleksandrov.recommendeddiploma.model.FactorizationData;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SVDRecommendationService extends BaseRecommendationAlgorithm {

    @Autowired
    private RedisService redisService;
    @Autowired
    private RecommendationSettingsRepository recommendationSettingsRepository;

    public static final String SVD_MODEL_KEY = "svd-model";

    @Autowired
    private DataLoader dataLoader;

    private DataModel dataModel;

    @Override
    public boolean supports(String algorithmType) {
        return "svd".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                                RecommendationSettings settings, boolean useCache) throws Exception {
        List<RecommendedItem> recommendedItems = recommend(userId, limit, filtering, settings, useCache);
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }

        return recommendations;
    }

    public List<RecommendedItem> recommend(String userId, int limit, boolean filtering,
                                           RecommendationSettings settings, boolean useCache) throws Exception {
        Recommender recommender = loadOrTrainRecommender(settings, useCache);

        // Поддержка фильтрации через cast
        if (recommender instanceof CustomSVDRecommender) {
            return ((CustomSVDRecommender) recommender)
                    .recommend(Long.parseLong(userId), limit, null, filtering); // !filtering -> includeKnownItems
        } else {
            // fallback
            return recommender.recommend(Long.parseLong(userId), limit);
        }
    }

    public Recommender loadOrTrainRecommender(RecommendationSettings settings, boolean useCache) throws Exception {
        dataModel = dataLoader.getDataModel();

        Factorization factorization;

        if (redisService.exists(SVD_MODEL_KEY) && useCache) {
            factorization = loadFactorizationFromRedis();
        } else {
            factorization = trainAndCacheFactorization(dataModel, settings, useCache);
        }

        return new CustomSVDRecommender(dataModel, factorization);
    }

    public Factorization trainAndCacheFactorization(DataModel dataModel,
                                                    RecommendationSettings settings, boolean useCache) throws Exception {
        int numFeatures = Integer.parseInt(settings.getParameters().get("numFeatures").toString());
        Factorizer factorizer = new ALSWRFactorizer(dataModel, numFeatures, 0.05, 5);
        Factorization factorization = factorizer.factorize();
        if (useCache) {
            saveFactorizationToRedis(factorization);
        }
        return factorization;
    }

    @Override
    public String retrainModel() throws Exception {
        RecommendationSettings settings = recommendationSettingsRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Настройки рекомендаций не найдены"));
        redisService.deleteModel(SVD_MODEL_KEY);
        redisService.evictRecommendationsByAlgorithm("svd");
        trainAndCacheFactorization(dataLoader.getDataModel(), settings, true);
        return "Retrain successfully";
    }

    public void saveFactorizationToRedis(Factorization factorization) {
        Map<Long, Integer> userIDMapping = new HashMap<>();
        Map<Long, Integer> itemIDMapping = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : factorization.getUserIDMappings()) {
            userIDMapping.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Long, Integer> entry : factorization.getItemIDMappings()) {
            itemIDMapping.put(entry.getKey(), entry.getValue());
        }

        FactorizationData factorizationData = new FactorizationData(userIDMapping, itemIDMapping,
                factorization.allUserFeatures(), factorization.allItemFeatures());

        redisService.saveModel(SVD_MODEL_KEY, factorizationData);
    }

    public Factorization loadFactorizationFromRedis() {
        FactorizationData factorizationData = (FactorizationData) redisService.getModel(SVD_MODEL_KEY);
        if (factorizationData == null) {
            return null;
        }

        FastByIDMap<Integer> userIDMapping = new FastByIDMap<>();
        for (Map.Entry<Long, Integer> entry : factorizationData.getUserIDMapping().entrySet()) {
            userIDMapping.put(entry.getKey(), entry.getValue());
        }

        FastByIDMap<Integer> itemIDMapping = new FastByIDMap<>();
        for (Map.Entry<Long, Integer> entry : factorizationData.getItemIDMapping().entrySet()) {
            itemIDMapping.put(entry.getKey(), entry.getValue());
        }

        return new Factorization(userIDMapping, itemIDMapping,
                factorizationData.getUserFeatures(), factorizationData.getItemFeatures());
    }
}
