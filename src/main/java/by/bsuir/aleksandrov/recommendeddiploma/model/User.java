package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "users")
@Data
public class User {
    @Id
    private String userId;
    private Map<String, Object> data;
    private List<Preference> preferences;

    public User(String userId, Map<String, Object> data) {
        this.userId = userId;
        this.data = data;
    }
}
