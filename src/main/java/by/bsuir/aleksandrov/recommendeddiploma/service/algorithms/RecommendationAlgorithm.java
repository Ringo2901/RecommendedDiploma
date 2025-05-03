package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;

import java.util.List;
import java.util.Map;

public interface RecommendationAlgorithm {
    boolean supports(String algorithmType);
    List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering,
                                         RecommendationSettings settings, boolean useCache) throws Exception;
    Map<String, Double> evaluateModel(int limit, RecommendationSettings settings) throws Exception;
    String retrainModel() throws Exception;
}
