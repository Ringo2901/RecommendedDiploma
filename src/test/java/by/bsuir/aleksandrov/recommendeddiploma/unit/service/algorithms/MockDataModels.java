package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import java.util.List;

public class MockDataModels {
    public static DataModel simpleModel() throws Exception {
        FastByIDMap<PreferenceArray> preferences = new FastByIDMap<>();

        preferences.put(1L, new GenericUserPreferenceArray(List.of(
                new GenericPreference(1L, 101L, 4.0f),
                new GenericPreference(1L, 102L, 3.0f)
        )));

        preferences.put(2L, new GenericUserPreferenceArray(List.of(
                new GenericPreference(2L, 101L, 5.0f),
                new GenericPreference(2L, 103L, 2.0f)
        )));

        return new GenericDataModel(preferences);
    }
}

