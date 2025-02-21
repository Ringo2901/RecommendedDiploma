package by.bsuir.aleksandrov.recommendeddiploma.controller;


import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.SchemaRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final SchemaValidator schemaValidator;

    public UserController(UserRepository userRepository, SchemaValidator schemaValidator) {
        this.userRepository = userRepository;
        this.schemaValidator = schemaValidator;
    }

    @PostMapping("/add")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        return validateAndSaveUser(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        Optional<User> user = userRepository.findByUserId(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<?> createUsers(@RequestBody List<User> users) {
        for (User user : users) {
            ResponseEntity<?> response = validateAndSaveUser(user);
            if (!response.getStatusCode().is2xxSuccessful()) return response;
        }
        return ResponseEntity.ok(userRepository.saveAll(users));
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        return userRepository.findByUserId(id).map(user -> {
            user.setData(updatedUser.getData());
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (userRepository.existsByUserId(id)) {
            userRepository.deleteByUserId(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearUsers() {
        userRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validateAndSaveUser(User user) {
        if (!schemaValidator.validate("user", user.getData())) {
            return ResponseEntity.badRequest().body("Ошибка: данные пользователя не соответствуют схеме!");
        }
        return ResponseEntity.ok(userRepository.save(user));
    }
}