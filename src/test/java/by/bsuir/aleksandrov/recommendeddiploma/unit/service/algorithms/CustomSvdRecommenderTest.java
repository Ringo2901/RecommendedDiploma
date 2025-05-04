package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd.CustomSVDRecommender;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorization;
import org.apache.mahout.cf.taste.model.*;
import org.apache.mahout.cf.taste.recommender.*;
import org.apache.mahout.cf.taste.similarity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomSvdRecommenderTest {

    private CustomSVDRecommender recommender;
    private DataModel dataModel;
    private Factorization factorization;
    private IDRescorer rescorer;

    @BeforeEach
    void setUp() throws TasteException {
        dataModel = mock(DataModel.class);
        factorization = mock(Factorization.class);
        recommender = new CustomSVDRecommender(dataModel, factorization);
        rescorer = mock(IDRescorer.class);
    }

    @Test
    void testRecommend_noRescorer_withKnownItems() throws TasteException {
        long userID = 1L;
        int howMany = 3;
        LongPrimitiveIterator itemIterator = mock(LongPrimitiveIterator.class);
        when(itemIterator.hasNext()).thenReturn(true, true, true, false);
        when(itemIterator.nextLong()).thenReturn(101L, 102L, 103L);

        when(dataModel.getItemIDs()).thenReturn(itemIterator);

        when(dataModel.getPreferenceValue(userID, 101L)).thenReturn(4.0f);
        when(factorization.getUserFeatures(userID)).thenReturn(new double[]{1.0, 0.5});
        when(factorization.getItemFeatures(101L)).thenReturn(new double[]{0.5, 1.0});
        when(factorization.getItemFeatures(102L)).thenReturn(new double[]{0.3, 0.7});
        when(factorization.getItemFeatures(103L)).thenReturn(new double[]{0.4, 0.8});

        List<RecommendedItem> recommendations = recommender.recommend(userID, howMany);

        assertNotNull(recommendations);
    }

    @Test
    void testRecommend_withFiltering() throws TasteException {
        long userID = 1L;
        int howMany = 3;
        LongPrimitiveIterator itemIterator = mock(LongPrimitiveIterator.class);
        when(itemIterator.hasNext()).thenReturn(true, true, true, false);
        when(itemIterator.nextLong()).thenReturn(101L, 102L, 103L);

        when(dataModel.getItemIDs()).thenReturn(itemIterator);
        when(dataModel.getPreferenceValue(userID, 101L)).thenReturn(4.0f); // This item should be filtered
        when(factorization.getUserFeatures(userID)).thenReturn(new double[]{1.0, 0.5});
        when(factorization.getItemFeatures(101L)).thenReturn(new double[]{0.5, 1.0});
        when(factorization.getItemFeatures(102L)).thenReturn(new double[]{0.3, 0.7});
        when(factorization.getItemFeatures(103L)).thenReturn(new double[]{0.4, 0.8});

        List<RecommendedItem> recommendations = recommender.recommend(userID, howMany, true);

        assertNotNull(recommendations);
    }

    @Test
    void testRecommend_withRescorer() throws TasteException {
        long userID = 1L;
        int howMany = 3;
        LongPrimitiveIterator itemIterator = mock(LongPrimitiveIterator.class);
        when(itemIterator.hasNext()).thenReturn(true, true, true, false);
        when(itemIterator.nextLong()).thenReturn(101L, 102L, 103L);

        when(dataModel.getItemIDs()).thenReturn(itemIterator);
        when(dataModel.getPreferenceValue(userID, 101L)).thenReturn(4.0f);
        when(factorization.getUserFeatures(userID)).thenReturn(new double[]{1.0, 0.5});
        when(factorization.getItemFeatures(101L)).thenReturn(new double[]{0.5, 1.0});
        when(factorization.getItemFeatures(102L)).thenReturn(new double[]{0.3, 0.7});
        when(factorization.getItemFeatures(103L)).thenReturn(new double[]{0.4, 0.8});
        when(rescorer.isFiltered(102L)).thenReturn(false);

        List<RecommendedItem> recommendations = recommender.recommend(userID, howMany, rescorer);

        assertNotNull(recommendations);
    }

    @Test
    void testEstimatePreference() throws TasteException {
        long userID = 1L;
        long itemID = 101L;

        when(factorization.getUserFeatures(userID)).thenReturn(new double[]{1.0, 0.5});
        when(factorization.getItemFeatures(itemID)).thenReturn(new double[]{0.5, 1.0});

        float estimatedPreference = recommender.estimatePreference(userID, itemID);

        assertEquals(1.0f * 0.5f + 0.5f * 1.0f, estimatedPreference, 0.01);
    }

    @Test
    void testEstimatePreference_whenNaN() throws TasteException {
        long userID = 1L;
        long itemID = 101L;

        when(factorization.getUserFeatures(userID)).thenReturn(new double[]{1.0, 0.5});
        when(factorization.getItemFeatures(itemID)).thenReturn(new double[]{Double.NaN, 0.0});

        float estimatedPreference = recommender.estimatePreference(userID, itemID);

        assertTrue(Float.isNaN(estimatedPreference));
    }

    @Test
    void testRecommend_errorHandling() throws TasteException {
        when(dataModel.getItemIDs()).thenThrow(new RuntimeException("Data model error"));

        assertThrows(TasteException.class, () -> {
            recommender.recommend(1L, 3);
        });
    }

    @Test
    void testSetPreference_unsupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            recommender.setPreference(1L, 101L, 5.0f);
        });
    }

    @Test
    void testRemovePreference_unsupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            recommender.removePreference(1L, 101L);
        });
    }
}
