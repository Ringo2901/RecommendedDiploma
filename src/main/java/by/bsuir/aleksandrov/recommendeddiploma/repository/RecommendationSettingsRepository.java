package by.bsuir.aleksandrov.recommendeddiploma.repository;

import by.bsuir.aleksandrov.recommendeddiploma.model.RecommendationSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RecommendationSettingsRepository extends MongoRepository<RecommendationSettings, String> {
    Optional<RecommendationSettings> findFirstByOrderByIdDesc();
}
