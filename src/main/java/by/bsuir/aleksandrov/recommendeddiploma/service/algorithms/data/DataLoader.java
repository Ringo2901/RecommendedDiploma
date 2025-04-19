package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.data;

import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import java.io.*;
import java.util.List;

@Service
public class DataLoader {
    @Autowired
    private UserRepository userRepository;

    @Getter
    private volatile DataModel dataModel;

    @PostConstruct
    public void init() {
        try {
            this.dataModel = loadUserDataModel();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при загрузке модели данных", e);
        }
    }

    private DataModel loadUserDataModel() throws Exception {
        File file = new File("user_preferences.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                if (user.getPreferences() != null) {
                    for (Preference preference : user.getPreferences()) {
                        String userId = sanitizeId(user.getUserId());
                        String itemId = sanitizeId(preference.getItemId());
                        writer.write(userId + "," + itemId + "," + preference.getRating());
                        writer.newLine();
                    }
                }
            }
        }

        return new FileDataModel(file);
    }

    private String sanitizeId(String id) {
        return isNumeric(id) ? id : id.substring(1);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
