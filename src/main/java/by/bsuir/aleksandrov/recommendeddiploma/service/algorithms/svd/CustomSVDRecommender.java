package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.GenericRecommendedItem;
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
    public List<RecommendedItem> recommend(long userID, int howMany) throws TasteException {
        return recommend(userID, howMany, null, true);
    }

    @Override
    public List<RecommendedItem> recommend(long userID, int howMany, boolean includeKnownItems) throws TasteException {
        return recommend(userID, howMany, null, includeKnownItems);
    }

    @Override
    public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer) throws TasteException {
        return recommend(userID, howMany, rescorer, true);
    }

    @Override
    public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer, boolean includeKnownItems) throws TasteException {
        List<RecommendedItem> recommendations = new ArrayList<>();
        Set<Long> itemIDs = new HashSet<>();

        try {
            dataModel.getItemIDs().forEachRemaining(itemIDs::add);

            for (Long itemId : itemIDs) {
                // Пропускаем уже оценённые товары, если фильтрация включена
                if (includeKnownItems && dataModel.getPreferenceValue(userID, itemId) != null) {
                    continue;
                }

                // Пропускаем, если рескорер фильтрует
                if (rescorer != null && rescorer.isFiltered(itemId)) {
                    continue;
                }

                float estimate = estimatePreference(userID, itemId);
                if (!Float.isNaN(estimate)) {
                    if (rescorer != null) {
                        estimate = (float) rescorer.rescore(itemId, estimate);
                    }
                    recommendations.add(new GenericRecommendedItem(itemId, estimate));
                }
            }
        } catch (Exception e) {
            throw new TasteException("Error during recommendation generation", e);
        }

        recommendations.sort(Comparator.comparingDouble(RecommendedItem::getValue).reversed());
        return recommendations.subList(0, Math.min(howMany, recommendations.size()));
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
        // No-op
    }
}
