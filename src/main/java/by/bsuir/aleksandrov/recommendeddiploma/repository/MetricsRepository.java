package by.bsuir.aleksandrov.recommendeddiploma.repository;

import by.bsuir.aleksandrov.recommendeddiploma.model.Metrics;
import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricsRepository extends MongoRepository<Metrics, String> {
    Optional<Metrics> findMetricsByName(String name);
}
