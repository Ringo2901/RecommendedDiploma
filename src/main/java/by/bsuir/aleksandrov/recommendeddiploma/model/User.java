package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "users")
@Data
public class User {
    @Id
    private String userId;
    private Map<String, Object> data;

    public User(String userId, Map<String, Object> data) {
        this.userId = userId;
        this.data = data;
    }
}
