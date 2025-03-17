package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms;

import java.util.List;

public interface RecommendationAlgorithm {
    boolean supports(String algorithmType);
    List<String> generateRecommendations(String userId, int limit, int offset) throws Exception;
    double evaluateModel() throws Exception;
}
