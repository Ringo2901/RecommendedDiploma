package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.model.SchemaField;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaValidatorTest {

    private SchemaRepository schemaRepository;
    private SchemaValidator schemaValidator;

    @BeforeEach
    void setUp() {
        schemaRepository = mock(SchemaRepository.class);
        schemaValidator = new SchemaValidator(schemaRepository);
    }

    @Test
    void validate_shouldReturnTrue_whenDataIsValid() {
        String entityType = "User";
        Map<String, Object> data = Map.of(
                "name", "John",
                "age", "30",
                "active", "true",
                "score", "12.5"
        );

        Schema schema = new Schema();
        schema.setEntityType(entityType);
        schema.setFields(List.of(
                new SchemaField("name", "String"),
                new SchemaField("age", "Integer"),
                new SchemaField("active", "Boolean"),
                new SchemaField("score", "Double")
        ));

        when(schemaRepository.findByEntityType(entityType)).thenReturn(Optional.of(schema));

        boolean result = schemaValidator.validate(entityType, data);
        assertTrue(result);
    }

    @Test
    void validate_shouldReturnFalse_whenSchemaNotFound() {
        when(schemaRepository.findByEntityType("Unknown")).thenReturn(Optional.empty());

        boolean result = schemaValidator.validate("Unknown", Map.of());
        assertFalse(result);
    }

    @Test
    void validate_shouldReturnFalse_whenFieldMissing() {
        String entityType = "User";
        Map<String, Object> data = Map.of("name", "John");

        Schema schema = new Schema();
        schema.setEntityType(entityType);
        schema.setFields(List.of(
                new SchemaField("name", "String"),
                new SchemaField("age", "Integer")
        ));

        when(schemaRepository.findByEntityType(entityType)).thenReturn(Optional.of(schema));

        boolean result = schemaValidator.validate(entityType, data);
        assertFalse(result);
    }

    @Test
    void validate_shouldReturnFalse_whenTypeMismatch() {
        String entityType = "User";
        Map<String, Object> data = Map.of(
                "name", "John",
                "age", "not-a-number"
        );

        Schema schema = new Schema();
        schema.setEntityType(entityType);
        schema.setFields(List.of(
                new SchemaField("name", "String"),
                new SchemaField("age", "Integer")
        ));

        when(schemaRepository.findByEntityType(entityType)).thenReturn(Optional.of(schema));

        boolean result = schemaValidator.validate(entityType, data);
        assertFalse(result);
    }

    @Test
    void validate_shouldReturnFalse_whenValueIsNotString() {
        String entityType = "User";
        Map<String, Object> data = Map.of(
                "age", 25 // not a String!
        );

        Schema schema = new Schema();
        schema.setEntityType(entityType);
        schema.setFields(List.of(
                new SchemaField("age", "Integer")
        ));

        when(schemaRepository.findByEntityType(entityType)).thenReturn(Optional.of(schema));

        boolean result = schemaValidator.validate(entityType, data);
        assertFalse(result);
    }

    @Test
    void validate_shouldReturnFalse_onUnsupportedType() {
        String entityType = "User";
        Map<String, Object> data = Map.of("level", "10");

        Schema schema = new Schema();
        schema.setEntityType(entityType);
        schema.setFields(List.of(new SchemaField("level", "UnknownType")));

        when(schemaRepository.findByEntityType(entityType)).thenReturn(Optional.of(schema));

        boolean result = schemaValidator.validate(entityType, data);
        assertFalse(result);
    }
}

