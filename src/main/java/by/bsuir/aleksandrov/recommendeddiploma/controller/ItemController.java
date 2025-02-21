package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final SchemaValidator schemaValidator;

    public ItemController(ItemRepository itemRepository, SchemaValidator schemaValidator) {
        this.itemRepository = itemRepository;
        this.schemaValidator = schemaValidator;
    }
    @PostMapping("/add")
    public ResponseEntity<?> createItem(@RequestBody Item item) {
        return validateAndSaveItem(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable String id) {
        Optional<Item> item = itemRepository.findByItemId(id);
        return item.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> createItems(@RequestBody List<Item> items) {
        for (Item item : items) {
            ResponseEntity<?> response = validateAndSaveItem(item);
            if (!response.getStatusCode().is2xxSuccessful()) return response;
        }
        return ResponseEntity.ok(itemRepository.saveAll(items));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable String id, @RequestBody Item updatedItem) {
        return itemRepository.findByItemId(id).map(item -> {
            item.setData(updatedItem.getData());
            itemRepository.save(item);
            return ResponseEntity.ok(item);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        if (itemRepository.existsByItemId(id)) {
            itemRepository.deleteByItemId(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearItems() {
        itemRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validateAndSaveItem(Item item) {
        if (!schemaValidator.validate("item", item.getData())) {
            return ResponseEntity.badRequest().body("Ошибка: данные товара не соответствуют схеме!");
        }
        return ResponseEntity.ok(itemRepository.save(item));
    }
}

