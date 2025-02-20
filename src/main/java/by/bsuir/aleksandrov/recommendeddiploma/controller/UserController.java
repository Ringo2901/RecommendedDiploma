package by.bsuir.aleksandrov.recommendeddiploma.controller;


import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<User> addUser(@RequestBody Map<String, Object> request) {
        User user = new User();
        user.setUserId((String) request.get("userId"));
        user.setData((Map<String, Object>) request.get("data"));

        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        Optional<User> user = userRepository.findByUserId(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<List<User>> addUsers(@RequestBody List<Map<String, Object>> request) {
        List<User> users = request.stream().map(data -> {
            User user = new User();
            user.setUserId((String) data.get("userId"));
            user.setData((Map<String, Object>) data.get("data"));
            return user;
        }).toList();

        userRepository.saveAll(users);
        return ResponseEntity.ok(users);
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
}