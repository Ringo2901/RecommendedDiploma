package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<String>> getRecommendations(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        List<String> recommendations = recommendationService.getRecommendations(userId, limit, offset);
        return ResponseEntity.ok(recommendations);
    }
}
