package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SchemaRepository schemaRepository;

    public AdminController(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @GetMapping("/schemas")
    public ResponseEntity<List<Schema>> getSchemas() {
        return ResponseEntity.ok(schemaRepository.findAll());
    }

    @GetMapping("/schema/{entityType}")
    public ResponseEntity<Schema> getSchemaByType(@PathVariable String entityType) {
        Optional<Schema> schema = schemaRepository.findByEntityType(entityType);
        return schema.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/schema")
    public ResponseEntity<Schema> createOrUpdateSchema(@RequestBody Schema schema) {
        Schema savedSchema = schemaRepository.save(schema);
        return ResponseEntity.ok(savedSchema);
    }

    @DeleteMapping("/schema/{id}")
    public ResponseEntity<Void> deleteSchema(@PathVariable String id) {
        schemaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
