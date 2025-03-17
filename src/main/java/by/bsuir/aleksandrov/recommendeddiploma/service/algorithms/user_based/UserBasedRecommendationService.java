package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based;

import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserBasedRecommendationService implements RecommendationAlgorithm {
    @Autowired
    private DataLoader dataLoader;
    private DataModel dataModel;

    @Override
    public boolean supports(String algorithmType) {
        return "user_based".equalsIgnoreCase(algorithmType);
    }

    @Override
    public List<String> generateRecommendations(String userId, int limit, int offset) throws Exception {
        if (dataModel == null) {
            dataModel = dataLoader.loadUserDataModel();
        }
        List<String> recommendations = new ArrayList<>();
        try {
            UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, similarity, dataModel);
            Recommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, similarity);
            if (!isNumeric(userId)) {
                userId = userId.substring(1);
            }
            List<RecommendedItem> recommendedItems = recommender.recommend(Long.parseLong(userId), limit);
            for (RecommendedItem item : recommendedItems) {
                recommendations.add(String.valueOf(item.getItemID()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recommendations;
    }

    @Override
    public double evaluateModel() throws Exception {
        if (dataModel == null) {
            dataModel = dataLoader.loadUserDataModel();
        }
        try {
            RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
            return evaluator.evaluate(recommender -> new GenericUserBasedRecommender(dataModel, new NearestNUserNeighborhood(10, new PearsonCorrelationSimilarity(dataModel), dataModel), new PearsonCorrelationSimilarity(dataModel)), null, dataModel, 0.7, 1.0);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
