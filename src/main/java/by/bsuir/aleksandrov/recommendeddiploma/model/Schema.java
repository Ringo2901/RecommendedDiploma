package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "schemas")
@Data
public class Schema {
    @Id
    private String id;
    private String type;
    private Map<String, String> fields;
}
