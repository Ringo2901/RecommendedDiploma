package by.bsuir.aleksandrov.recommendeddiploma.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.RecommendationAlgorithm;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationSettingsRepository settingsRepository;
    private final List<RecommendationAlgorithm> algorithms;

    public RecommendationService(RecommendationSettingsRepository settingsRepository,
                                 List<RecommendationAlgorithm> algorithms) {
        this.settingsRepository = settingsRepository;
        this.algorithms = algorithms;
    }

    public List<String> getRecommendations(String userId, int limit, int offset) {
        RecommendationSettings settings = settingsRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Настройки рекомендаций не найдены"));

        return algorithms.stream()
                .filter(algo -> algo.supports(String.valueOf(settings.getAlgorithm())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Алгоритм не найден"))
                .generateRecommendations(userId, limit, offset);
    }
}
