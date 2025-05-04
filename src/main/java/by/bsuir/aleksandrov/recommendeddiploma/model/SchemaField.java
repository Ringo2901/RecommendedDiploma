package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaField {
    private String name;
    private String type;
    private double weight;

    public SchemaField(String name, String type) {
        this.name = name;
        this.type = type;
        this.weight = 1;
    }

    public SchemaField(String name, Double weight) {
        this.name = name;
        this.type = "type";
        this.weight = weight;
    }
}

