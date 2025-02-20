package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "schemas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    @Id
    private String id;
    private String entityType;
    private List<SchemaField> fields;
}
