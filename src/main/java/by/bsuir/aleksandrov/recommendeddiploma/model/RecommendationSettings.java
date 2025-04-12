package by.bsuir.aleksandrov.recommendeddiploma.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "settings")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationSettings {
    @Id
    private String id;
    private RecommendationAlgorithmType algorithm;
    private Map<String, Object> parameters;

}
