package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.DatabaseMetrics;
import by.bsuir.aleksandrov.recommendeddiploma.model.Metrics;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import by.bsuir.aleksandrov.recommendeddiploma.repository.MetricsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.RecommendationSettingsRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.RecommendationService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AdminController {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private MetricsRepository metricsRepository;
    @Autowired
    private RecommendationSettingsRepository settingsRepository;

    @GetMapping("/admin")
    public String getAdminPanel(Model model) {
        deleteOldMetrics();

        List<DatabaseMetrics> metrics = getDatabaseMetrics();
        model.addAttribute("dbMetrics", metrics);

        return "admin";
    }

    private List<DatabaseMetrics> getDatabaseMetrics() {
        return mongoTemplate.findAll(DatabaseMetrics.class, "dbMetrics");
    }

    private void deleteOldMetrics() {
        long currentTime = System.currentTimeMillis();
        long oneHoursAgo = currentTime - 60 * 60 * 1000;

        mongoTemplate.getDb().getCollection("dbMetrics").deleteMany(
                new Document("timestamp", new Document("$lt", oneHoursAgo))
        );
    }

    @GetMapping("/admin/import")
    public String importPage() {
        return "admin-csv-import";
    }

    @GetMapping("/admin/settings")
    public String settingsPage() {
        return "settings";
    }

    @GetMapping("/admin/metrics")
    public String getMetrics(Model model) throws Exception {
        RecommendationSettings settings = settingsRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Настройки рекомендаций не найдены"));
        model.addAttribute("name", settings.getAlgorithm().name());
        Optional<Metrics> metrics = metricsRepository.findMetricsByName(settings.getAlgorithm().name());
        if (metrics.isPresent()) {
            model.addAttribute("metrics", metrics.get().getData());
        } else {
            Map<String, Map<Integer, Double>> metricsMap = calculateMetrics();
            Metrics result = new Metrics();
            result.setData(metricsMap);
            result.setName(settings.getAlgorithm().name());
            result.setTimestamp(LocalDateTime.now());
            metricsRepository.save(result);
            model.addAttribute("metrics", metricsMap);
        }
        return "admin-metrics";
    }

    private Map<String, Map<Integer, Double>> calculateMetrics() throws Exception {
        int minLimit = 10;
        int maxLimit = 110;
        int step = 20;

        Map<Integer, Double> precisionList = new HashMap<>();
        Map<Integer, Double> recallList = new HashMap<>();
        Map<Integer, Double> f1ScoreList = new HashMap<>();
        Map<Integer, Double> nDCGList = new HashMap<>();
        Map<Integer, Double> hitRateList = new HashMap<>();
        Map<Integer, Double> coverageList = new HashMap<>();
        Map<Integer, Double> personalizationList = new HashMap<>();

        for (int limit = minLimit; limit <= maxLimit; limit += step) {
            System.out.println("Limit : " + limit);
            Map<String, Double> metrics = recommendationService.evaluate(limit);
            precisionList.put(limit, metrics.get("precision"));
            recallList.put(limit, metrics.get("recall"));
            f1ScoreList.put(limit, metrics.get("f1Score"));
            nDCGList.put(limit, metrics.get("nDCG"));
            hitRateList.put(limit, metrics.get("hitRate"));
            coverageList.put(limit, metrics.get("coverage"));
            personalizationList.put(limit, metrics.get("personalization"));
        }

        Map<String, Map<Integer, Double>> metrics = new HashMap<>();
        metrics.put("precision", precisionList);
        metrics.put("recall", recallList);
        metrics.put("f1Score", f1ScoreList);
        metrics.put("nDCG", nDCGList);
        metrics.put("hitRate", hitRateList);
        metrics.put("coverage", coverageList);
        metrics.put("personalization", personalizationList);

        return metrics;
    }
}
