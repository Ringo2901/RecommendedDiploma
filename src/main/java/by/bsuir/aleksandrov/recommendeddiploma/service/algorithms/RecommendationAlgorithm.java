package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms;

import java.util.List;
import java.util.Map;

public interface RecommendationAlgorithm {
    boolean supports(String algorithmType);
    List<String> generateRecommendations(String userId, int limit, int offset, boolean filtering) throws Exception;
    Map<String, Double> evaluateModel(int limit) throws Exception;
}
