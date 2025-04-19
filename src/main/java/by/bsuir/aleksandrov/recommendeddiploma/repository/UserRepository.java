package by.bsuir.aleksandrov.recommendeddiploma.repository;

import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findById(String id);
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}

