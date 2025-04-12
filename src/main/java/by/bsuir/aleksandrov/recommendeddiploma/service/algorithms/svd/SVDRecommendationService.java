package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd;

import by.bsuir.aleksandrov.recommendeddiploma.model.FactorizationData;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.RecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.BaseRecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
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
        List<RecommendedItem> recommendedItems = recommend(userId, limit);
        List<String> recommendations = new ArrayList<>();

        for (RecommendedItem item : recommendedItems) {
            recommendations.add(String.valueOf(item.getItemID()));
        }

        return recommendations;
    }

    public List<RecommendedItem> recommend(String userId, int limit) throws Exception {
        Recommender recommender = loadOrTrainRecommender();
        return recommender.recommend(Long.parseLong(userId), limit);
    }

    private Recommender loadOrTrainRecommender() throws Exception {
        dataModel = dataLoader.getDataModel(); // загружаем dataModel (из базы, файла и т.д.)

        Factorization factorization;

        if (redisService.exists(SVD_MODEL_KEY)) {
            factorization = loadFactorizationFromRedis();
        } else {
            factorization = trainAndCacheFactorization(dataModel);
        }

        return new CustomSVDRecommender(dataModel, factorization);
    }

    private Factorization trainAndCacheFactorization(DataModel dataModel) throws Exception {
        Factorizer factorizer = new ALSWRFactorizer(dataModel, 10, 0.05, 10);
        Factorization factorization = factorizer.factorize();

        saveFactorizationToRedis(factorization); // сериализация Factorization
        return factorization;
    }

    @Override
    public String retrainModel() throws Exception {
        redisService.deleteModel(SVD_MODEL_KEY);
        redisService.evictRecommendationsByAlgorithm("svd");
        trainAndCacheFactorization(dataLoader.getDataModel());
        return "Retrain successfully";
    }

    public void saveFactorizationToRedis(Factorization factorization) {
        Map<Long, Integer> userIDMapping = new HashMap<>();
        Map<Long, Integer> itemIDMapping = new HashMap<>();

        // Преобразуем FastByIDMap в обычные HashMap для сериализации
        for (Map.Entry<Long, Integer> entry : factorization.getUserIDMappings()) {
            userIDMapping.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Long, Integer> entry : factorization.getItemIDMappings()) {
            itemIDMapping.put(entry.getKey(), entry.getValue());
        }

        // Сохраняем в Redis
        FactorizationData factorizationData = new FactorizationData(userIDMapping, itemIDMapping,
                factorization.allUserFeatures(), factorization.allItemFeatures());

        redisService.saveModel(SVD_MODEL_KEY, factorizationData);
    }

    public Factorization loadFactorizationFromRedis() {
        // Получаем данные из Redis
        FactorizationData factorizationData = (FactorizationData) redisService.getModel(SVD_MODEL_KEY);
        if (factorizationData == null) {
            return null;
        }

        // Восстанавливаем FastByIDMap
        FastByIDMap<Integer> userIDMapping = new FastByIDMap<>();
        for (Map.Entry<Long, Integer> entry : factorizationData.getUserIDMapping().entrySet()) {
            userIDMapping.put(entry.getKey(), entry.getValue());
        }

        FastByIDMap<Integer> itemIDMapping = new FastByIDMap<>();
        for (Map.Entry<Long, Integer> entry : factorizationData.getItemIDMapping().entrySet()) {
            itemIDMapping.put(entry.getKey(), entry.getValue());
        }

        // Восстанавливаем Factorization
        return new Factorization(userIDMapping, itemIDMapping,
                factorizationData.getUserFeatures(), factorizationData.getItemFeatures());
    }


}
