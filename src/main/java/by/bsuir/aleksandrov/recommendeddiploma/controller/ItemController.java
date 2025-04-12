package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaValidator;
import by.bsuir.aleksandrov.recommendeddiploma.service.redis.RedisService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final SchemaValidator schemaValidator;
    private final RedisService redisService;

    public ItemController(ItemRepository itemRepository, SchemaValidator schemaValidator, RedisService redisService) {
        this.itemRepository = itemRepository;
        this.schemaValidator = schemaValidator;
        this.redisService = redisService;
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
        redisService.evictAllRecommendations();
        List<Item> validItems = items.stream()
                .filter(item -> {
                    boolean isValid = schemaValidator.validate("Item", item.getData());
                    if (!isValid) {
                        System.out.println("Пропущен элемент с itemId: " + item.getItemId() + " из-за несоответствия схеме.");
                    }
                    return isValid;
                })
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: ни один товар не соответствует схеме.");
        }

        return ResponseEntity.ok(itemRepository.saveAll(validItems));
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

    @PostMapping("/upload-csv")
    public ResponseEntity<?> uploadItemsFromCSV(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: Файл пуст.");
        }
        redisService.evictAllRecommendations();
        List<Item> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String itemId = record.get("itemId");
                    Map<String, Object> data = new HashMap<>();

                    for (String header : record.toMap().keySet()) {
                        if (!header.equals("itemId")) {
                            data.put(header, record.get(header));
                        }
                    }

                    Item item = new Item(itemId, data);

                    if (!schemaValidator.validate("Item", data)) {
                        errors.add("Ошибка: Товар с itemId=" + itemId + " не соответствует схеме.");
                        continue;
                    }

                    items.add(item);
                } catch (Exception e) {
                    errors.add("Ошибка при обработке записи: " + record.toString());
                }
            }
/*
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(errors);
            }
*/
            itemRepository.saveAll(items);
            return ResponseEntity.ok("Файл успешно загружен. Добавлено товаров: " + items.size() + " Пропущено товаров:" + errors.size());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при обработке файла: " + e.getMessage());
        }
    }

    private ResponseEntity<?> validateAndSaveItem(Item item) {
        if (!schemaValidator.validate("item", item.getData())) {
            System.out.println("Пропущен товар с itemId: " + item.getItemId() + " из-за несоответствия схеме.");
            return ResponseEntity.badRequest().body("Ошибка: данные товара не соответствуют схеме!");
        }
        return ResponseEntity.ok(itemRepository.save(item));
    }
}
