package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data.DataLoader;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.io.*;
import java.util.*;

class DataLoaderTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private User user;
    @Mock
    private Preference preference;
    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataLoader = new DataLoader();
        dataLoader.userRepository = userRepository;  // Инжектим мокированный репозиторий
    }

    @Test
    void testInit_success() throws Exception {
        // Подготавливаем данные
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        List<by.bsuir.aleksandrov.recommendeddiploma.model.Preference> preferences = List.of(preference);
        when(user.getPreferences()).thenReturn(preferences);
        when(preference.getItemId()).thenReturn("1");
        when(preference.getRating()).thenReturn(5.0);
        when(user.getUserId()).thenReturn("1");

        // Мокируем записи в файл
        File tempFile = File.createTempFile("test_data", ".csv");
        tempFile.deleteOnExit();
        dataLoader.init();

        // Проверяем, что файл был создан и содержит данные
        File file = new File("user_preferences.csv");
        assertTrue(file.exists());

        // Проверяем корректность содержимого файла
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            assertNotNull(line);
            assertTrue(line.contains("1,1,5.0"));
        }
    }

    @Test
    void testSanitizeId_numericId() {
        // Проверяем, что ID без изменений, если оно числовое
        assertEquals("123", dataLoader.sanitizeId("123"));
    }

    @Test
    void testSanitizeId_nonNumericId() {
        // Проверяем, что первый символ удаляется, если ID не числовое
        assertEquals("123", dataLoader.sanitizeId("u123"));
    }

    @Test
    void testLoadUserDataModel() throws Exception {
        // Подготавливаем данные
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        when(user.getPreferences()).thenReturn(List.of(preference));
        when(preference.getItemId()).thenReturn("1");
        when(preference.getRating()).thenReturn(5.0);
        when(user.getUserId()).thenReturn("1");

        // Мокируем запись в файл
        File tempFile = File.createTempFile("test_data", ".csv");
        tempFile.deleteOnExit();

        // Проверяем, что DataModel загружается правильно
        FileDataModel dataModel = (FileDataModel) dataLoader.loadUserDataModel();
        assertNotNull(dataModel);
    }

    @Test
    void testLoadUserDataModel_exceptionHandling() {
        // Подготавливаем тест, который вызовет исключение
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        // Проверяем, что в случае ошибки выбрасывается исключение
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dataLoader.init();
        });
        assertEquals("Ошибка при загрузке модели данных", exception.getMessage());
    }
}
