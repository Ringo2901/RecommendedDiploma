package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationAlgorithmType;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.svd.SVDRecommendationService.SVD_MODEL_KEY;

@RestController
@RequestMapping("/admin/recommendation-settings")
public class AdminRecommendationController {

    private final RecommendationSettingsRepository settingsRepository;
    private final RedisService redisService;

    public AdminRecommendationController(RecommendationSettingsRepository settingsRepository, RedisService redisService) {
        this.settingsRepository = settingsRepository;
        this.redisService = redisService;
    }

    @GetMapping
    public ResponseEntity<RecommendationSettings> getSettings() {
        return ResponseEntity.ok(settingsRepository.findById("global").orElseGet(this::defaultSettings));
    }

    @PostMapping
    public ResponseEntity<String> updateSettings(@RequestBody RecommendationSettings newSettings) {
        newSettings.setId("global");
        settingsRepository.save(newSettings);
        redisService.deleteModel(SVD_MODEL_KEY);
        redisService.evictAllRecommendations();
        return ResponseEntity.ok("Настройки обновлены");
    }

    private RecommendationSettings defaultSettings() {
        Map<String, Object> defaultParams = new HashMap<>();

        return new RecommendationSettings("global", RecommendationAlgorithmType.TF_IDF, defaultParams);
    }
}
