package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.user_based;

import by.bsuir.aleksandrov.recommendeddiploma.model.Preference;
import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import by.bsuir.aleksandrov.recommendeddiploma.repository.UserRepository;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

@Service
public class DataLoader {
    @Autowired
    UserRepository userRepository;

    public DataModel loadUserDataModel() throws Exception {
        File file = new File("user_preferences.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                if (user.getPreferences() != null) {
                    for (Preference preference : user.getPreferences()) {
                        String userId = user.getUserId();
                        String itemId = preference.getItemId();

                        if (!isNumeric(userId)) {
                            userId = userId.substring(1);
                        }

                        if (!isNumeric(itemId)) {
                            itemId = itemId.substring(1);
                        }

                        writer.write(userId + "," + itemId + "," + preference.getRating());
                        writer.newLine();
                    }
                }
            }
        }

        return new FileDataModel(file);
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
