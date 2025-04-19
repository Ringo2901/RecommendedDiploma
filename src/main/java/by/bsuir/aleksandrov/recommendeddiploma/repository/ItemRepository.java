package by.bsuir.aleksandrov.recommendeddiploma.repository;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ItemRepository extends MongoRepository<Item, String> {
    Optional<Item> findById(String id);
    Optional<Item> findByItemId(String itemId);
    boolean existsByItemId(String itemId);
    void deleteByItemId(String itemId);
}
