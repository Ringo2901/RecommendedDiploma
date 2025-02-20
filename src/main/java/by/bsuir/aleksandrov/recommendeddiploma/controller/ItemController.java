package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<Item> addItem(@RequestBody Map<String, Object> request) {
        Item item = new Item();
        item.setItemId((String) request.get("itemId"));
        item.setData((Map<String, Object>) request.get("data"));

        itemRepository.save(item);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItem(@PathVariable String id) {
        Optional<Item> item = itemRepository.findByItemId(id);
        return item.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<List<Item>> addItems(@RequestBody List<Map<String, Object>> request) {
        List<Item> items = request.stream().map(data -> {
            Item item = new Item();
            item.setItemId((String) data.get("itemId"));
            item.setData((Map<String, Object>) data.get("data"));
            return item;
        }).toList();

        itemRepository.saveAll(items);
        return ResponseEntity.ok(items);
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
}

