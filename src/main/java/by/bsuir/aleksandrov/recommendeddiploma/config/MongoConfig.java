package by.bsuir.aleksandrov.recommendeddiploma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "by.bsuir.aleksandrov.recommendeddiploma.repository")
public class MongoConfig {
}
