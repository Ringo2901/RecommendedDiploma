package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.util.*;

public class CustomSVDRecommender implements Recommender {

    private final DataModel dataModel;
    private final Factorization factorization;

    public CustomSVDRecommender(DataModel dataModel, Factorization factorization) {
        this.dataModel = dataModel;
        this.factorization = factorization;
    }

    @Override
    public List<RecommendedItem> recommend(long userID, int howMany) {
        List<RecommendedItem> recommendations = new ArrayList<>();
        Set<Long> itemIDs = new HashSet<>();
        try {
            dataModel.getItemIDs().forEachRemaining(itemIDs::add);
            for (Long itemId : itemIDs) {
                if (dataModel.getPreferenceValue(userID, itemId) != null) continue;

                float estimate = estimatePreference(userID, itemId);
                recommendations.add(new org.apache.mahout.cf.taste.impl.recommender.GenericRecommendedItem(itemId, estimate));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        recommendations.sort(Comparator.comparingDouble(RecommendedItem::getValue).reversed());
        return recommendations.subList(0, Math.min(howMany, recommendations.size()));
    }

    @Override
    public List<RecommendedItem> recommend(long l, int i, boolean b) throws TasteException {
        return List.of();
    }

    @Override
    public List<RecommendedItem> recommend(long l, int i, IDRescorer idRescorer) throws TasteException {
        return List.of();
    }

    @Override
    public List<RecommendedItem> recommend(long l, int i, IDRescorer idRescorer, boolean b) throws TasteException {
        return List.of();
    }

    @Override
    public float estimatePreference(long userID, long itemID) {
        try {
            double[] userFeatures = factorization.getUserFeatures(userID);
            double[] itemFeatures = factorization.getItemFeatures(itemID);
            double score = 0.0;
            for (int i = 0; i < userFeatures.length; i++) {
                score += userFeatures[i] * itemFeatures[i];
            }
            return (float) score;
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    @Override
    public void setPreference(long userID, long itemID, float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePreference(long userID, long itemID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataModel getDataModel() {
        return dataModel;
    }

    @Override
    public void refresh(Collection<Refreshable> collection) {

    }
}

