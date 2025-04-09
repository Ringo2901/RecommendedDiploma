package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based;

import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.common.RandomUtils;

import java.util.*;

public class DataSplitter {

    public static Map<String, DataModel> splitDataModel(DataModel originalModel, double trainRatio) throws Exception {
        RandomUtils.useTestSeed(); // Фиксируем случайность для повторяемости
        FastByIDMap<PreferenceArray> trainPrefs = new FastByIDMap<>();
        FastByIDMap<PreferenceArray> testPrefs = new FastByIDMap<>();
        Random random = new Random();

        LongPrimitiveIterator userIterator = originalModel.getUserIDs();
        while (userIterator.hasNext()) {
            long userId = userIterator.nextLong();
            List<Preference> userPreferences = new ArrayList<>();

            for (Preference pref : originalModel.getPreferencesFromUser(userId)) {
                userPreferences.add(new GenericPreference(userId, pref.getItemID(), pref.getValue()));
            }

            Collections.shuffle(userPreferences, random);
            int trainSize = (int) (trainRatio * userPreferences.size());

            if (trainSize > 0) {
                trainPrefs.put(userId, new GenericUserPreferenceArray(userPreferences.subList(0, trainSize)));
            }
            if (trainSize < userPreferences.size()) {
                testPrefs.put(userId, new GenericUserPreferenceArray(userPreferences.subList(trainSize, userPreferences.size())));
            }
        }

        return Map.of(
                "train", new GenericDataModel(trainPrefs),
                "test", new GenericDataModel(testPrefs)
        );
    }
}
