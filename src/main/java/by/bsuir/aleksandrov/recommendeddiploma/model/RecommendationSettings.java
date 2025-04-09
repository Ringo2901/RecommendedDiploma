package by.bsuir.aleksandrov.recommendeddiploma.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSettings {
    @Id
    private String id;
    private RecommendationAlgorithmType algorithm;
    private Map<String, Object> parameters;

}
