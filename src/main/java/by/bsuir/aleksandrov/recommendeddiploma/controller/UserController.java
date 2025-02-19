package by.bsuir.aleksandrov.recommendeddiploma.controller;


import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
}