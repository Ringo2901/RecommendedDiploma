package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "items")
@Data
public class Item {
    @Id
    private String id;
    private String itemId;
    private Map<String, Object> data;
}
