package by.bsuir.aleksandrov.recommendeddiploma.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class SchemaValidator {
    private final SchemaRepository schemaRepository;

    public SchemaValidator(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    public boolean validate(String entityType, Map<String, Object> data) {
        Optional<Schema> schemaOpt = schemaRepository.findByEntityType(entityType);
        if (schemaOpt.isEmpty()) {
            return false;
        }
        Schema schema = schemaOpt.get();

        for (var field : schema.getFields()) {
            if (!data.containsKey(field.getName())) {
                return false;
            }
            if (!isTypeValid(data.get(field.getName()), field.getType())) {
                return false;
            }
        }
        return true;
    }

    private boolean isTypeValid(Object value, String type) {
        if (!(value instanceof String strValue)) {
            return false;
        }

        return switch (type) {
            case "String" -> true;
            case "Integer" -> strValue.matches("-?\\d+");
            case "Boolean" -> strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("false");
            case "Double" -> strValue.matches("-?\\d+(\\.\\d+)?");
            default -> false;
        };
    }

}
