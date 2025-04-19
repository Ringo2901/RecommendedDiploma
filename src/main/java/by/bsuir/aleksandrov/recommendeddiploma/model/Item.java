package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "items")
@Data
public class Item {
    @Id
    private String itemId;
    private Map<String, Object> data;

    public Item(String itemId, Map<String, Object> data) {
        this.itemId = itemId;
        this.data = data;
    }

    public String toText(List<SchemaField> fields){
        StringBuilder result = new StringBuilder();
        for (SchemaField schemaField : fields) {
            String field_data = data.get(schemaField.getName()).toString();
            for (int i = 0; i < (int) schemaField.getWeight(); i++) {
                result.append(field_data);
                result.append(" ");
            }
        }
        return result.toString();
    }
}
