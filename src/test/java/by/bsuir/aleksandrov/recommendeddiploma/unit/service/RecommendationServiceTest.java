package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationAlgorithmType;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.RecommendationService;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {

    private RecommendationSettingsRepository settingsRepository;
    private RecommendationAlgorithm mockAlgorithm;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(RecommendationSettingsRepository.class);
        mockAlgorithm = mock(RecommendationAlgorithm.class);

        List<RecommendationAlgorithm> algorithms = List.of(mockAlgorithm);
        recommendationService = new RecommendationService(settingsRepository, algorithms);
    }

    @Test
    void getRecommendations_shouldReturnRecommendations() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setAlgorithm(RecommendationAlgorithmType.USER_BASED);

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));
        when(mockAlgorithm.supports("USER_BASED")).thenReturn(true);
        when(mockAlgorithm.generateRecommendations("user1", 10, 0, true, settings, true))
                .thenReturn(List.of("item1", "item2"));

        List<String> result = recommendationService.getRecommendations("user1", 10, 0);

        assertEquals(List.of("item1", "item2"), result);
        verify(mockAlgorithm).generateRecommendations("user1", 10, 0, true, settings, true);
    }

    @Test
    void getRecommendations_shouldThrowIfSettingsNotFound() {
        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> recommendationService.getRecommendations("user1", 10, 0));
        assertEquals("Настройки рекомендаций не найдены", ex.getMessage());
    }

    @Test
    void getRecommendations_shouldThrowIfAlgorithmNotFound() {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setAlgorithm(RecommendationAlgorithmType.SVD);

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));
        when(mockAlgorithm.supports("SVD")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> recommendationService.getRecommendations("user1", 10, 0));
        assertEquals("Алгоритм не найден", ex.getMessage());
    }

    @Test
    void evaluate_shouldReturnEvaluationMap() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setAlgorithm(RecommendationAlgorithmType.SVD);

        Map<String, Double> metrics = Map.of("precision", 0.8);

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));
        when(mockAlgorithm.supports("SVD")).thenReturn(true);
        when(mockAlgorithm.evaluateModel(20, settings)).thenReturn(metrics);

        Map<String, Double> result = recommendationService.evaluate(20);

        assertEquals(0.8, result.get("precision"));
    }

    @Test
    void retrainModel_shouldRetrainIfSVD() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setAlgorithm(RecommendationAlgorithmType.SVD);

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));
        when(mockAlgorithm.supports("SVD")).thenReturn(true);
        when(mockAlgorithm.retrainModel()).thenReturn("Model retrained");

        String result = recommendationService.retrainModel();

        assertEquals("Model retrained", result);
    }

    @Test
    void retrainModel_shouldReturnMessageIfNotSVD() throws Exception {
        RecommendationSettings settings = new RecommendationSettings();
        settings.setAlgorithm(RecommendationAlgorithmType.USER_BASED);

        when(settingsRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(settings));

        String result = recommendationService.retrainModel();

        assertEquals("Not svd algorithm!", result);
        verify(mockAlgorithm, never()).retrainModel();
    }
}

