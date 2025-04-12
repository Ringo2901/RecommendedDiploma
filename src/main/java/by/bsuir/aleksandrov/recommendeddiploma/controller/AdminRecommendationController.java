package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationAlgorithmType;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        redisService.evictAllRecommendations();
        return ResponseEntity.ok("Настройки обновлены");
    }

    private RecommendationSettings defaultSettings() {
        Map<String, Object> defaultParams = new HashMap<>();
        defaultParams.put("numFactors", 50);
        defaultParams.put("numIterations", 100);

        return new RecommendationSettings("global", RecommendationAlgorithmType.TF_IDF, defaultParams);
    }
}
