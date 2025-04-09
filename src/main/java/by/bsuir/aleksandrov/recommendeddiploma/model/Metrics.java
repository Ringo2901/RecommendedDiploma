package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document("metrics")
@Data
public class Metrics {
    private String name;
    private Map<String, Map<Integer, Double>> data;
    private LocalDateTime timestamp;
}
