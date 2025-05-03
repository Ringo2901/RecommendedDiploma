package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.DatabaseMetrics;
import by.bsuir.aleksandrov.recommendeddiploma.model.Metrics;
import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationAlgorithmType;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;

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
        model.addAttribute("optimalParam", 0);
        setXAxisName(model, settings);
        Optional<Metrics> metrics = metricsRepository.findMetricsByName(settings.getAlgorithm().name());
        if (metrics.isPresent()) {
            model.addAttribute("metrics", metrics.get().getData());
        } else {
            Map<String, Map<Integer, Double>> metricsMap = calculateMetrics(settings);
            Metrics result = new Metrics();
            result.setData(metricsMap);
            result.setName(settings.getAlgorithm().name());
            result.setTimestamp(LocalDateTime.now());
            metricsRepository.save(result);
            model.addAttribute("metrics", metricsMap);
        }
        return "admin-metrics";
    }

    @PostMapping("/admin/metrics/recalculate")
    public String recalculateMetrics() {
        metricsRepository.deleteAll();
        return "redirect:/admin/metrics";
    }

    @PostMapping("/admin/metrics/auto-weight")
    public String calculateOptimalParameter(
            @RequestParam Map<String, String> weights,
            Model model
    ) {
        RecommendationSettings settings = settingsRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Настройки рекомендаций не найдены"));
        Optional<Metrics> optionalMetrics = metricsRepository.findMetricsByName(settings.getAlgorithm().name());

        if (optionalMetrics.isEmpty()) {
            model.addAttribute("error", "Метрики не найдены для выбранного алгоритма.");
            return "redirect:/admin/metrics";
        }

        Map<String, Double> weightMap = new HashMap<>();
        for (Map.Entry<String, String> entry : weights.entrySet()) {
            try {
                weightMap.put(entry.getKey().toLowerCase(), Double.parseDouble(entry.getValue()));
            } catch (NumberFormatException e) {
                weightMap.put(entry.getKey().toLowerCase(), 0.0);
            }
        }

        Metrics metrics = optionalMetrics.get();
        Map<String, Map<Integer, Double>> interpolatedMetrics = new HashMap<>();

        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;

        // Найдём общий диапазон параметров
        for (Map<Integer, Double> values : metrics.getData().values()) {
            for (int param : values.keySet()) {
                globalMin = Math.min(globalMin, param);
                globalMax = Math.max(globalMax, param);
            }
        }

        // Интерполируем каждую метрику
        for (Map.Entry<String, Map<Integer, Double>> metricEntry : metrics.getData().entrySet()) {
            String metricName = metricEntry.getKey();
            Map<Integer, Double> interpolated = getIntegerDoubleMap(metricEntry, globalMin, globalMax);

            interpolatedMetrics.put(metricName, interpolated);
        }

        // Вычислим итоговые баллы
        Map<Integer, Double> scorePerParam = new HashMap<>();
        for (int i = globalMin; i <= globalMax; i++) {
            double totalScore = 0.0;
            for (String metricName : interpolatedMetrics.keySet()) {
                Map<Integer, Double> values = interpolatedMetrics.get(metricName);
                double weight = weightMap.getOrDefault("weights[" + metricName.toLowerCase() + "]", 0.0);
                double value = values.getOrDefault(i, 0.0);
                totalScore += value * weight;
            }
            scorePerParam.put(i, totalScore);
        }

        int bestParam = scorePerParam.entrySet().stream()
                .filter(e -> !Double.isNaN(e.getValue()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);


        model.addAttribute("optimalParam", bestParam);
        model.addAttribute("optimalParamScore", scorePerParam.getOrDefault(bestParam, 0.0));
        model.addAttribute("weights", weightMap);
        model.addAttribute("name", settings.getAlgorithm().name());
        model.addAttribute("metrics", metrics.getData());
        setXAxisName(model, settings);

        return "admin-metrics";
    }

    private static Map<Integer, Double> getIntegerDoubleMap(Map.Entry<String, Map<Integer, Double>> metricEntry, int globalMin, int globalMax) {
        Map<Integer, Double> original = new TreeMap<>(metricEntry.getValue());
        Map<Integer, Double> interpolated = new HashMap<>();

        for (int i = globalMin; i <= globalMax; i++) {
            if (original.containsKey(i)) {
                interpolated.put(i, original.get(i));
            } else {
                // Найдём ближайшие соседние точки
                Integer lower = null, upper = null;
                for (Integer key : original.keySet()) {
                    if (key < i) lower = key;
                    else if (key > i) {
                        upper = key;
                        break;
                    }
                }

                if (lower != null && upper != null) {
                    double y1 = original.get(lower);
                    double y2 = original.get(upper);
                    double interpolatedValue = y1 + (y2 - y1) * ((double) (i - lower) / (upper - lower));
                    interpolated.put(i, interpolatedValue);
                } else if (lower != null) {
                    interpolated.put(i, original.get(lower));
                } else if (upper != null) {
                    interpolated.put(i, original.get(upper));
                }
            }
        }
        return interpolated;
    }


    private void setXAxisName(Model model, RecommendationSettings settings) {
        switch (settings.getAlgorithm()) {
            case USER_BASED:
                model.addAttribute("xAxisName", "Число соседей");
                break;
            case SVD:
                model.addAttribute("xAxisName", "Число параметров");
                break;
            case TF_IDF:
                model.addAttribute("xAxisName", "Число товаров");
                break;
            case ITEM_BASED:
            default:
                model.addAttribute("xAxisName", "Число рекомендаций");
                break;
        }
    }


    private Map<String, Map<Integer, Double>> calculateMetrics(RecommendationSettings settings) throws Exception {
        int min = 10;
        int max = 110;
        int step = 20;

        RecommendationAlgorithmType algorithm = settings.getAlgorithm();
        Map<String, Object> parameters = settings.getParameters();
        Map<String, Object> oldParams = settings.getParameters();

        String varyingParam;
        switch (algorithm) {
            case USER_BASED:
                min = 1;
                max = 201;
                varyingParam = "numNeighbors";
                break;
            case SVD:
                min = 20;
                max = 200;
                varyingParam = "numFeatures";
                break;
            case TF_IDF:
                max = 160;
                step = 30;
                varyingParam = "numItems";
                break;
            case ITEM_BASED:
            default:
                varyingParam = "limit";
                break;
        }

        Map<Integer, Double> precisionList = new HashMap<>();
        Map<Integer, Double> recallList = new HashMap<>();
        Map<Integer, Double> f1ScoreList = new HashMap<>();
        Map<Integer, Double> nDCGList = new HashMap<>();
        Map<Integer, Double> hitRateList = new HashMap<>();
        Map<Integer, Double> coverageList = new HashMap<>();
        Map<Integer, Double> personalizationList = new HashMap<>();

        for (int value = min; value <= max; value += step) {
            System.out.println("Testing " + varyingParam + " = " + value);

            if (algorithm != RecommendationAlgorithmType.ITEM_BASED) {
                parameters.put(varyingParam, value);
                settings.setParameters(parameters);
                settingsRepository.save(settings);
            }

            Map<String, Double> metrics = recommendationService.evaluate(
                    algorithm == RecommendationAlgorithmType.ITEM_BASED ? value : 10
            );

            precisionList.put(value, metrics.get("precision"));
            recallList.put(value, metrics.get("recall"));
            f1ScoreList.put(value, metrics.get("f1Score"));
            nDCGList.put(value, metrics.get("nDCG"));
            hitRateList.put(value, metrics.get("hitRate"));
            coverageList.put(value, metrics.get("coverage"));
            personalizationList.put(value, metrics.get("personalization"));
        }
        settings.setParameters(oldParams);
        settingsRepository.save(settings);
        Map<String, Map<Integer, Double>> result = new HashMap<>();
        result.put("precision", precisionList);
        result.put("recall", recallList);
        result.put("f1Score", f1ScoreList);
        result.put("nDCG", nDCGList);
        result.put("hitRate", hitRateList);
        result.put("coverage", coverageList);
        result.put("personalization", personalizationList);

        return result;
    }

}
