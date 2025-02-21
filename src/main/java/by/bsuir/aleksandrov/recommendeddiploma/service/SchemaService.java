package by.bsuir.aleksandrov.recommendeddiploma.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaService {
    private final SchemaRepository schemaRepository;

    public SchemaService(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    public List<Schema> getAllSchemas() {
        return schemaRepository.findAll();
    }

    public void saveSchema(Schema schema) {
        schemaRepository.save(schema);
    }

    public void deleteSchema(String id) {
        schemaRepository.deleteById(id);
    }
}
