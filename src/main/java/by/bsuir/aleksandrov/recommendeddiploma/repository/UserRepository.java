package by.bsuir.aleksandrov.recommendeddiploma.repository;

import by.bsuir.aleksandrov.recommendeddiploma.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}

