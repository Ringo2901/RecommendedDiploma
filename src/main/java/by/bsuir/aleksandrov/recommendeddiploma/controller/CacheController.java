package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller()
@RequestMapping("/admin/cache")
public class CacheController {
    @Autowired
    private RedisService redisService;

    @GetMapping
    public String cachePage(Model model) {
        Map<String, Integer> cacheSizes = redisService.getRecommendationCacheSizesByAlgorithm();
        boolean svdModelExists = redisService.exists("svd-model");

        model.addAttribute("cacheSizes", cacheSizes);
        model.addAttribute("svdModelExists", svdModelExists);
        model.addAttribute("currentAlgorithm", redisService.getCurrentAlgorithm());

        return "admin-cache";
    }

    @PostMapping("/clear/{algorithm}")
    public String clearAlgorithmCache(@PathVariable String algorithm) {
        redisService.evictRecommendationsByAlgorithm(algorithm);
        return "redirect:/admin/cache";
    }

    @PostMapping("/clear/svd-model")
    public String clearSVDModel() {
        redisService.deleteModel("svd-model");
        return "redirect:/admin/cache";
    }

    @PostMapping("/clear/all")
    public String clearAllCache() {
        redisService.clearAll();
        return "redirect:/admin/cache";
    }
}
