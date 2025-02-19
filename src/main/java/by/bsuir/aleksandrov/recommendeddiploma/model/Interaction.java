package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "interactions")
@Data
public class Interaction {
    @Id
    private String id;
    private String userId;
    private String itemId;
    private String action;
    private long timestamp;
}
