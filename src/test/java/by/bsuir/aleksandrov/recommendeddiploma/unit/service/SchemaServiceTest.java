package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaServiceTest {

    private SchemaRepository schemaRepository;
    private SchemaService schemaService;

    @BeforeEach
    void setUp() {
        schemaRepository = mock(SchemaRepository.class);
        schemaService = new SchemaService(schemaRepository);
    }

    @Test
    void getAllSchemas_shouldReturnAllSchemas() {
        List<Schema> mockSchemas = List.of(
                new Schema("1", "User", List.of()),
                new Schema("2", "Product", List.of())
        );

        when(schemaRepository.findAll()).thenReturn(mockSchemas);

        List<Schema> result = schemaService.getAllSchemas();

        assertEquals(2, result.size());
        assertEquals("User", result.get(0).getEntityType());
        verify(schemaRepository, times(1)).findAll();
    }

    @Test
    void saveSchema_shouldCallRepositorySave() {
        Schema schema = new Schema("1", "User", List.of());

        schemaService.saveSchema(schema);

        verify(schemaRepository, times(1)).save(schema);
    }

    @Test
    void deleteSchema_shouldCallRepositoryDeleteById() {
        String id = "1";

        schemaService.deleteSchema(id);

        verify(schemaRepository, times(1)).deleteById(id);
    }
}

