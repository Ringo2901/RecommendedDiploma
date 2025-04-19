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
}

