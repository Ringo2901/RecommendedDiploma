package by.bsuir.aleksandrov.recommendeddiploma.model;

import java.util.Map;

public class FactorizationData {
    private Map<Long, Integer> userIDMapping;
    private Map<Long, Integer> itemIDMapping;
    private double[][] userFeatures;
    private double[][] itemFeatures;

    public FactorizationData() {}
    // Конструктор, геттеры и сеттеры
    public FactorizationData(Map<Long, Integer> userIDMapping, Map<Long, Integer> itemIDMapping, double[][] userFeatures, double[][] itemFeatures) {
        this.userIDMapping = userIDMapping;
        this.itemIDMapping = itemIDMapping;
        this.userFeatures = userFeatures;
        this.itemFeatures = itemFeatures;
    }

    // Геттеры и сеттеры
    public Map<Long, Integer> getUserIDMapping() {
        return userIDMapping;
    }

    public void setUserIDMapping(Map<Long, Integer> userIDMapping) {
        this.userIDMapping = userIDMapping;
    }

    public Map<Long, Integer> getItemIDMapping() {
        return itemIDMapping;
    }

    public void setItemIDMapping(Map<Long, Integer> itemIDMapping) {
        this.itemIDMapping = itemIDMapping;
    }

    public double[][] getUserFeatures() {
        return userFeatures;
    }

    public void setUserFeatures(double[][] userFeatures) {
        this.userFeatures = userFeatures;
    }

    public double[][] getItemFeatures() {
        return itemFeatures;
    }

    public void setItemFeatures(double[][] itemFeatures) {
        this.itemFeatures = itemFeatures;
    }
}

