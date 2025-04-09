package by.bsuir.aleksandrov.recommendeddiploma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecommendedDiplomaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendedDiplomaApplication.class, args);
    }

}
