package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.ItemRepository;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaValidator;
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
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final SchemaValidator schemaValidator;
    private final ItemRepository itemRepository;

    public UserController(UserRepository userRepository, SchemaValidator schemaValidator, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.schemaValidator = schemaValidator;
        this.itemRepository = itemRepository;
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
        List<User> validUsers = users.stream()
                .filter(user -> {
                    boolean isValid = schemaValidator.validate("User", user.getData());
                    if (!isValid) {
                        System.out.println("Пропущен пользователь с userId: " + user.getUserId() + " из-за несоответствия схеме.");
                    }
                    return isValid;
                })
                .collect(Collectors.toList());

        if (validUsers.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: ни один пользователь не соответствует схеме.");
        }

        return ResponseEntity.ok(userRepository.saveAll(validUsers));
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
            System.out.println("Пропущен пользователь с userId: " + user.getUserId() + " из-за несоответствия схеме.");
            return ResponseEntity.badRequest().body("Ошибка: данные пользователя не соответствуют схеме!");
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<?> uploadUsersFromCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: Файл пуст.");
        }

        List<User> users = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String userId = record.get("userId"); // Предполагается, что CSV содержит userId
                    Map<String, Object> data = new HashMap<>();

                    for (String header : record.toMap().keySet()) {
                        if (!header.equals("userId")) {
                            data.put(header, record.get(header));
                        }
                    }

                    User user = new User(userId, data);

                    if (!schemaValidator.validate("User", data)) {
                        errors.add("Ошибка: Пользователь с userId=" + userId + " не соответствует схеме.");
                        continue;
                    }

                    users.add(user);
                } catch (Exception e) {
                    errors.add("Ошибка при обработке записи: " + record.toString());
                }
            }
/*
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(errors);
            }
*/
            userRepository.saveAll(users);
            return ResponseEntity.ok("Файл успешно загружен. Добавлено пользователей: " + users.size() + " Пропущено записей: " + errors.size());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при обработке файла: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/preferences")
    public ResponseEntity<?> addPreference(@PathVariable String userId, @RequestBody Preference preference) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: Пользователь не найден");
        }

        User user = userOptional.get();
        user.getPreferences().add(preference);
        userRepository.save(user);

        return ResponseEntity.ok("Предпочтение добавлено");
    }

    @PostMapping("/{userId}/preferences/bulk-add")
    public ResponseEntity<?> addPreferences(@PathVariable String userId, @RequestBody List<Preference> preferences) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: Пользователь не найден");
        }

        User user = userOptional.get();
        user.getPreferences().addAll(preferences);
        userRepository.save(user);

        return ResponseEntity.ok("Предпочтения добавлены");
    }

    @PostMapping("/upload-preferences")
    public ResponseEntity<?> uploadPreferencesFromCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: Файл пуст.");
        }

        List<String> errors = new ArrayList<>();
        int batchSize = 1000; // Размер пачки
        Map<String, List<Preference>> userPreferencesMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String userId = record.get("userId");
                    String itemId = record.get("itemId");
                    double rating = Double.parseDouble(record.get("rating"));

                    double minBound = record.isSet("minBound") && !record.get("minBound").isEmpty()
                            ? Double.parseDouble(record.get("minBound"))
                            : 0.0;
                    double maxBound = record.isSet("maxBound") && !record.get("maxBound").isEmpty()
                            ? Double.parseDouble(record.get("maxBound"))
                            : 10.0;

                    if (itemRepository.existsByItemId(itemId) && userRepository.existsByUserId(userId)) {
                        Preference preference = new Preference(itemId, rating, minBound, maxBound);
                        userPreferencesMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(preference);
                    } else {
                        if (errors.size() < 1000) {
                            errors.add("Пользователь с id: " + userId + " или товар с id: " + itemId + " не найдены!");
                        }
                    }

                    // Если накопилось batchSize записей - сохраняем в БД и очищаем список
                    if (userPreferencesMap.size() >= batchSize) {
                        saveUserPreferences(userPreferencesMap, errors);
                        userPreferencesMap.clear();
                    }
                } catch (NumberFormatException e) {
                    if (errors.size() < 1000) {
                        errors.add("Ошибка преобразования числа в строке: " + record.toString());
                    }
                } catch (Exception e) {
                    if (errors.size() < 1000) {
                        errors.add("Ошибка при обработке строки: " + record.toString() + " | " + e.getMessage());
                    }
                }
            }

            // Сохранение оставшихся записей
            if (!userPreferencesMap.isEmpty()) {
                saveUserPreferences(userPreferencesMap, errors);
            }

            return ResponseEntity.ok("Файл успешно загружен. Пропущено записей: " + errors.size());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при обработке файла: " + e.getMessage());
        }
    }

    /**
     * Метод для сохранения данных пользователей партиями
     */
    private void saveUserPreferences(Map<String, List<Preference>> userPreferencesMap, List<String> errors) {
        for (Map.Entry<String, List<Preference>> entry : userPreferencesMap.entrySet()) {
            String userId = entry.getKey();
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPreferences() == null) {
                    user.setPreferences(new ArrayList<>());
                }
                user.getPreferences().addAll(entry.getValue());
                userRepository.save(user);
            } else {
                errors.add("Ошибка: Пользователь с ID " + userId + " не найден.");
            }
        }
    }
}
